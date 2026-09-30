package dev.alinco8.xmmp.io

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier

*///? } else {
import net.minecraft.resources.ResourceLocation as Identifier

//? }

import dev.alinco8.xmmp.MC_CHUNK_SIZE
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.network.XMMPStreamCodec
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property

object TilePayloadCodec {
    const val VERSION = 2

    private val biomeKeyCodec = XMMPStreamCodec.resourceKey(Registries.BIOME)

    private class Palette<T>(private val map: MutableMap<T, Int>) {
        val entries = ArrayList<T>()

        fun indexOf(value: T) = map.getOrPut(value) {
            entries.add(value)
            entries.size - 1
        }
    }

    fun encode(buf: FriendlyByteBuf, tileData: TileData, blocks: Array<BlockData>) {
        val states = Palette<BlockState>(Reference2IntOpenHashMap())
        val biomes = Palette<ResourceKey<Biome>>(Object2IntOpenHashMap())

        val stateIdx = IntArray(blocks.size)
        val biomeIdx = IntArray(blocks.size)
        val overlayIdx = Array(blocks.size) { i ->
            val block = blocks[i]

            stateIdx[i] = states.indexOf(block.blockState)
            biomeIdx[i] = biomes.indexOf(block.biome)

            IntArray(block.overlays.size) { j ->
                states.indexOf(block.overlays[j].state)
            }
        }

        buf.writeInt(VERSION)
        TileData.codec.encode(buf, tileData)

        buf.writeVarInt(states.entries.size)
        states.entries.forEach {
            blockStateCodec.encode(buf, it)
        }

        buf.writeVarInt(biomes.entries.size)
        biomes.entries.forEach {
            biomeKeyCodec.encode(buf, it)
        }

        blocks.forEachIndexed { i, block ->
            buf.writeVarInt(stateIdx[i])
            buf.writeShort(block.height.toInt())
            buf.writeShort(block.topHeight.toInt())
            buf.writeVarInt(biomeIdx[i])
            buf.writeByte(block.lightLevel.toInt())
            buf.writeBoolean(block.glowing)

            buf.writeVarInt(block.overlays.size)
            block.overlays.forEachIndexed { j, overlay ->
                buf.writeVarInt(overlayIdx[i][j])
                buf.writeByte(overlay.light.toInt())
                buf.writeBoolean(overlay.glowing)
                buf.writeShort(overlay.opacity.toInt())
            }
        }
    }

    fun decode(buf: FriendlyByteBuf): Pair<TileData, Array<BlockData>>? = runCatching {
        val version = buf.readInt()
        if (version != VERSION) {
            LOGGER.warn("Received tile payload with unsupported version $version, expected $VERSION")

            return null
        }

        val tileData = TileData.codec.decode(buf)

        val stateCount = buf.readVarInt()
        val states = Array(stateCount) {
            blockStateCodec.decode(buf)
        }
        val biomeCount = buf.readVarInt()
        val biomes = Array(biomeCount) {
            biomeKeyCodec.decode(buf)
        }

        val blockData = Array(MC_CHUNK_SIZE * MC_CHUNK_SIZE) {
            val state = states[buf.readVarInt()]
            val height = buf.readShort()
            val topHeight = buf.readShort()
            val biome = biomes[buf.readVarInt()]
            val lightLevel = buf.readByte()
            val glowing = buf.readBoolean()

            val overlayCount = buf.readVarInt()
            val overlays = Array(overlayCount) {
                val overlayState = states[buf.readVarInt()]
                val overlayLight = buf.readByte()
                val overlayGlowing = buf.readBoolean()
                val overlayOpacity = buf.readShort()

                BlockOverlay(overlayState, overlayLight, overlayGlowing, overlayOpacity)
            }.toList()

            BlockData(state, height, topHeight, biome, lightLevel, glowing, overlays)
        }

        return tileData to blockData
    }.getOrNull()

    private val blockStateCodec = XMMPStreamCodec.of(
        { buf ->
            val blockId = Identifier /*?>=1.21||forge{*/.parse/*?}*/(buf.readUtf())
            val block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(Blocks.AIR)
            var state = block.defaultBlockState()

            val propsByName = state.properties.associateBy { it.name }

            val propCount = buf.readVarInt()
            repeat(propCount) {
                val name = buf.readUtf()
                val value = buf.readUtf()

                @Suppress("UNCHECKED_CAST")
                val prop = propsByName[name] as? Property<Comparable<Any>>? ?: return@repeat
                val actualValue = prop.getValue(value).orElse(null) ?: return@repeat

                state = state.trySetValue(prop, actualValue)
            }

            return@of state
        }, { buf, state ->
            buf.writeUtf(BuiltInRegistries.BLOCK.getKey(state.block).toString())

            val props = state.properties.toList()
            buf.writeVarInt(props.size)
            props.forEach { prop ->
                buf.writeUtf(prop.name)
                @Suppress("UNCHECKED_CAST")
                buf.writeUtf(
                    (prop as Property<Comparable<Any>>).getName(state.getValue(prop))
                )
            }
        }
    )

    data class BlockOverlay(
        val state: BlockState,
        val light: Byte,
        val glowing: Boolean,
        val opacity: Short,
    ) {
        companion object {
            const val MAX_OVERLAYS = 256
        }
    }

    data class TileData(
        val worldInterpretationVersion: Int,
    ) {
        companion object {
            val codec = with(XMMPStreamCodec) {
                composite(
                    int, TileData::worldInterpretationVersion,
                    ::TileData
                )
            }
        }
    }

    data class BlockData(
        val blockState: BlockState,
        val height: Short,
        val topHeight: Short,
        val biome: ResourceKey<Biome>,
        val lightLevel: Byte,
        val glowing: Boolean,
        val overlays: List<BlockOverlay>,
    )
}

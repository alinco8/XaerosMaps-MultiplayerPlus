package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.common.XMMPPacket
import dev.alinco8.xmmp.common.XMMPPacketType
import dev.alinco8.xmmp.loc
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import java.util.Objects

data class ChunkDataPacket(
    val chunkX: Int,
    val chunkZ: Int,
    val tileData: TileData,
    val blocks: Array<BlockData>,
    val dimension: String,
) : XMMPPacket<ChunkDataPacket>() {
    data class BlockOverlay(
        val state: BlockState,
        val light: Byte,
        val glowing: Boolean,
        val opacity: Short,
    ) {
        companion object {
            const val MAX_OVERLAYS = 256

            fun read(buf: FriendlyByteBuf) = BlockOverlay(
                Block.BLOCK_STATE_REGISTRY.byIdOrThrow(buf.readVarInt()),
                buf.readByte(),
                buf.readBoolean(),
                buf.readShort()
            )

            fun readList(buf: FriendlyByteBuf): List<BlockOverlay> {
                val size = buf.readInt()
                require(size in 0..MAX_OVERLAYS) { "Invalid overlay count: $size" }
                return List(size) { read(buf) }
            }
        }

        fun write(buf: FriendlyByteBuf) {
            buf.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(state))
            buf.writeByte(light.toInt())
            buf.writeBoolean(glowing)
            buf.writeShort(opacity.toInt())
        }
    }

    data class TileData(
        val worldInterpretationVersion: Int,
    ) {
        companion object {
            fun read(buf: FriendlyByteBuf) = TileData(buf.readInt())
        }

        fun write(buf: FriendlyByteBuf) {
            buf.writeInt(worldInterpretationVersion)
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
    ) {
        companion object {
            fun read(buf: FriendlyByteBuf) = BlockData(
                Block.BLOCK_STATE_REGISTRY.byIdOrThrow(buf.readVarInt()),
                buf.readShort(),
                buf.readShort(),
                buf.readResourceKey(Registries.BIOME),
                buf.readByte(),
                buf.readBoolean(),
                BlockOverlay.readList(buf)
            )

            fun readArray(buf: FriendlyByteBuf): Array<BlockData> {
                val size = buf.readInt()
                require(size == CHUNK_SIZE * CHUNK_SIZE) { "Invalid block count: $size" }
                return Array(size) { read(buf) }
            }
        }

        fun write(buf: FriendlyByteBuf) {
            buf.writeVarInt(Block.BLOCK_STATE_REGISTRY.getId(blockState))
            buf.writeShort(height.toInt())
            buf.writeShort(topHeight.toInt())
            buf.writeResourceKey(biome)
            buf.writeByte(lightLevel.toInt())
            buf.writeBoolean(glowing)

            buf.writeInt(overlays.size)
            overlays.forEach { it.write(buf) }
        }
    }

    companion object : XMMPPacketType<ChunkDataPacket>() {
        const val CHUNK_SIZE = 16

        override fun id() = loc("chunk_data")

        override fun decode(buf: FriendlyByteBuf) = ChunkDataPacket(
            buf.readInt(),
            buf.readInt(),
            TileData.read(buf),
            BlockData.readArray(buf),
            buf.readUtf()
        )

        override fun encode(buf: FriendlyByteBuf, packet: ChunkDataPacket) {
            buf.writeInt(packet.chunkX)
            buf.writeInt(packet.chunkZ)
            packet.tileData.write(buf)
            buf.writeInt(packet.blocks.size)
            packet.blocks.forEach { it.write(buf) }
            buf.writeUtf(packet.dimension)
        }
    }

    init {
        require(blocks.size == CHUNK_SIZE * CHUNK_SIZE) {
            "Expected exactly ${CHUNK_SIZE * CHUNK_SIZE} blocks, but got ${blocks.size}"
        }
    }

    override val encode = Companion::encode
    override val payloadType = Companion.payloadType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkDataPacket) return false
        return chunkX == other.chunkX && chunkZ == other.chunkZ && blocks.contentEquals(other.blocks)
                && tileData == other.tileData && dimension == other.dimension
    }

    override fun hashCode() =
        Objects.hash(chunkX, chunkZ, blocks.contentHashCode(), tileData, dimension)

}

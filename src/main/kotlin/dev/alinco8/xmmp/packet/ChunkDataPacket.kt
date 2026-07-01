package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.loc
import java.util.*
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.state.BlockState

data class ChunkDataPacket(
    val chunkX: Int,
    val chunkZ: Int,
    val tileData: TileData,
    val blocks: Array<BlockData>,
    val dimension: String,
) : CustomPacketPayload {
    data class BlockOverlay(
        val state: BlockState,
        val light: Byte,
        val glowing: Boolean,
        val opacity: Short,
    ) {
        companion object {
            val OVERLAY_STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.fromCodec(BlockState.CODEC), BlockOverlay::state,
                ByteBufCodecs.BYTE, BlockOverlay::light,
                ByteBufCodecs.BOOL, BlockOverlay::glowing,
                ByteBufCodecs.SHORT, BlockOverlay::opacity,
                ::BlockOverlay
            )

            val OVERLAYS_STREAM_CODEC = StreamCodec.of<FriendlyByteBuf, List<BlockOverlay>>(
                { buf, list ->
                    buf.writeInt(list.size)
                    list.forEach { OVERLAY_STREAM_CODEC.encode(buf, it) }
                },
                { buf ->
                    val size = buf.readInt()
                    require(size in 0..MAX_OVERLAYS) { "Invalid overlay count: $size" }
                    List(size) { OVERLAY_STREAM_CODEC.decode(buf) }
                }
            )

            const val MAX_OVERLAYS = 256
        }
    }

    data class TileData(
        val worldInterpretationVersion: Int,
    ) {
        companion object {
            val STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, TileData::worldInterpretationVersion,
                ::TileData
            )
        }
    }

    data class BlockData(
        val blockState: BlockState,
        val height: Short,
        val topHeight: Short,
        val biome: ResourceKey<Biome>,
        val lightLevel: Byte,
        val glowing: Boolean,
        val overlays: List<BlockOverlay>
    ) {
        companion object {
            val STREAM_CODEC: StreamCodec<FriendlyByteBuf, BlockData> =
                StreamCodec.of({ buf, packet ->
                    ByteBufCodecs.fromCodec(BlockState.CODEC).encode(buf, packet.blockState)
                    buf.writeShort(packet.height.toInt())
                    buf.writeShort(packet.topHeight.toInt())
                    buf.writeResourceKey(packet.biome)
                    buf.writeByte(packet.lightLevel.toInt())
                    buf.writeBoolean(packet.glowing)
                    BlockOverlay.OVERLAYS_STREAM_CODEC.encode(buf, packet.overlays)
                }, { buf ->
                    BlockData(
                        ByteBufCodecs.fromCodec(BlockState.CODEC).decode(buf),
                        buf.readShort(),
                        buf.readShort(),
                        buf.readResourceKey(Registries.BIOME),
                        buf.readByte(),
                        buf.readBoolean(),
                        BlockOverlay.OVERLAYS_STREAM_CODEC.decode(buf)
                    )
                })


            val CHUNK_BLOCKS_STREAM_CODEC: StreamCodec<FriendlyByteBuf, Array<BlockData>> =
                StreamCodec.of(
                    { buf, arr ->
                        buf.writeInt(arr.size)
                        arr.forEach { STREAM_CODEC.encode(buf, it) }
                    },
                    { buf ->
                        val size = buf.readInt()
                        require(size == CHUNK_SIZE * CHUNK_SIZE) {
                            "Invalid block count: $size"
                        }
                        Array(size) { STREAM_CODEC.decode(buf) }
                    }
                )
        }
    }

    companion object {
        const val CHUNK_SIZE = 16

        val TYPE = CustomPacketPayload.Type<ChunkDataPacket>(loc("chunk_data"))

        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, ChunkDataPacket> = StreamCodec.composite(
            ByteBufCodecs.INT, ChunkDataPacket::chunkX,
            ByteBufCodecs.INT, ChunkDataPacket::chunkZ,
            TileData.STREAM_CODEC, ChunkDataPacket::tileData,
            BlockData.CHUNK_BLOCKS_STREAM_CODEC, ChunkDataPacket::blocks,
            ByteBufCodecs.STRING_UTF8, ChunkDataPacket::dimension,
            ::ChunkDataPacket
        )
    }

    init {
        require(blocks.size == CHUNK_SIZE * CHUNK_SIZE) {
            "Expected exactly ${CHUNK_SIZE * CHUNK_SIZE} blocks, but got ${blocks.size}"
        }
    }

    override fun type() = TYPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChunkDataPacket) return false
        return chunkX == other.chunkX && chunkZ == other.chunkZ && blocks.contentEquals(other.blocks)
                && tileData == other.tileData && dimension == other.dimension
    }

    override fun hashCode() =
        Objects.hash(chunkX, chunkZ, blocks.contentHashCode(), tileData, dimension)
}

package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import java.util.Objects
import net.minecraft.world.level.Level

data class S2CChunkData(
    val dimension: ResourceKey<Level>,
    val chunkPos: ChunkKey,
    val revision: Long,
    val payload: ByteArray,
) : XMMPPacket<S2CChunkData>(Companion) {
    companion object : Type<S2CChunkData>(S2CChunkData::class.java) {
        override fun id() = packetId("s2c_chunk_data")

        override val codec = with(XMMPStreamCodec) {
            composite(
                resourceKey(Registries.DIMENSION), S2CChunkData::dimension,
                ChunkKey.codec, S2CChunkData::chunkPos,
                long, S2CChunkData::revision,
                byteArray, S2CChunkData::payload,
                ::S2CChunkData,
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is S2CChunkData) return false
        return dimension == other.dimension &&
                chunkPos == other.chunkPos &&
                revision == other.revision &&
                payload.contentEquals(other.payload)
    }

    override fun hashCode() = Objects.hash(
        dimension,
        chunkPos,
        revision,
        payload.contentHashCode()
    )
}

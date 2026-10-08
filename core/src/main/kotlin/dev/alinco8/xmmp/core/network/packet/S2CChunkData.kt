package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.ChunkKey
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec
import java.util.Objects

data class S2CChunkData(
    val dimension: ResourceId,
    val layer: SyncLayer,
    val chunkPos: ChunkKey,
    val revision: Long,
    val payload: ByteArray,
) : XMMPPacket<S2CChunkData>(Companion) {
    companion object : Type<S2CChunkData>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                id, S2CChunkData::dimension,
                SyncLayer.codec, S2CChunkData::layer,
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
                layer == other.layer &&
                chunkPos == other.chunkPos &&
                revision == other.revision &&
                payload.contentEquals(other.payload)
    }

    override fun hashCode() = Objects.hash(
        dimension,
        layer,
        chunkPos,
        revision,
        payload.contentHashCode()
    )
}

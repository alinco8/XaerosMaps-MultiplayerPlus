package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.ChunkKey
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec
import java.util.Objects

data class C2SChunkUpload(
    val dimension: ResourceId,
    val layer: SyncLayer,
    val chunkPos: ChunkKey,
    val seq: Long,
    val payload: ByteArray,
) : XMMPPacket<C2SChunkUpload>(Companion) {
    companion object : Type<C2SChunkUpload>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                id, C2SChunkUpload::dimension,
                SyncLayer.codec, C2SChunkUpload::layer,
                ChunkKey.codec, C2SChunkUpload::chunkPos,
                long, C2SChunkUpload::seq,
                byteArray, C2SChunkUpload::payload,
                ::C2SChunkUpload
            )
        }
    }

    override fun equals(other: Any?) = other is C2SChunkUpload
            && dimension == other.dimension
            && layer == other.layer
            && chunkPos == other.chunkPos
            && seq == other.seq
            && payload.contentEquals(other.payload)

    override fun hashCode() = Objects.hash(dimension, layer, chunkPos, seq, payload)
}

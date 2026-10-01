package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.SyncLayer
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import java.util.Objects
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

data class C2SChunkUpload(
    val dimension: ResourceKey<Level>,
    val layer: SyncLayer,
    val chunkPos: ChunkKey,
    val seq: Long,
    val payload: ByteArray,
) : XMMPPacket<C2SChunkUpload>(Companion) {
    companion object : Type<C2SChunkUpload>(C2SChunkUpload::class.java) {
        override fun id() = packetId("c2s_chunk_upload")
        override val codec = with(XMMPStreamCodec) {
            composite(
                resourceKey(Registries.DIMENSION), C2SChunkUpload::dimension,
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

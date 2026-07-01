package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.loc
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class C2SChunkRowRequestPacket(
    val regionX: Int,
    val regionZ: Int,
    val row: Int,
    val missingFlags: Int,
    val dimension: String,
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<C2SChunkRowRequestPacket>(loc("c2s_chunk_row_request"))
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, C2SChunkRowRequestPacket> =
            StreamCodec.composite(
                ByteBufCodecs.INT, C2SChunkRowRequestPacket::regionX,
                ByteBufCodecs.INT, C2SChunkRowRequestPacket::regionZ,
                ByteBufCodecs.INT, C2SChunkRowRequestPacket::row,
                ByteBufCodecs.INT, C2SChunkRowRequestPacket::missingFlags,
                ByteBufCodecs.STRING_UTF8, C2SChunkRowRequestPacket::dimension,
                ::C2SChunkRowRequestPacket
            )
    }

    override fun type() = TYPE
}

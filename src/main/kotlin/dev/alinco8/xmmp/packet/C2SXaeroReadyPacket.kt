package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.loc
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class C2SXaeroReadyPacket(
    val dimension: String
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<C2SXaeroReadyPacket>(loc("c2s_xaero_ready"))
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, C2SXaeroReadyPacket> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, C2SXaeroReadyPacket::dimension,
            ::C2SXaeroReadyPacket
        )
    }

    override fun type() = TYPE
}

package dev.alinco8.xmmp.packet

import net.minecraft.network.FriendlyByteBuf

//? if >=1.20.5 {
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import dev.alinco8.xmmp.loc

//? }

data class C2SXaeroReadyPacket(
    val dimension: String
)/*?>=1.20.5>>'{'*/ : CustomPacketPayload  {
    //? if >=1.20.5 {
    companion object {
        val TYPE = CustomPacketPayload.Type<C2SXaeroReadyPacket>(loc("c2s_xaero_ready"))
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, C2SXaeroReadyPacket> = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, C2SXaeroReadyPacket::dimension,
            ::C2SXaeroReadyPacket
        )
    }

    override fun type() = TYPE

    //? } else {
    /*companion object {
        fun decode(buf: FriendlyByteBuf) = C2SXaeroReadyPacket(
            buf.readUtf()
        )
    }

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(dimension)
    }

    *///? }
}

package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.common.XMMPPacket
import dev.alinco8.xmmp.common.XMMPPacketType
import dev.alinco8.xmmp.loc
import net.minecraft.network.FriendlyByteBuf

data class C2SXaeroReadyPacket(
    val dimension: String
) : XMMPPacket<C2SXaeroReadyPacket>() {
    companion object : XMMPPacketType<C2SXaeroReadyPacket>() {
        override fun id() = loc("c2s_xaero_ready")

        override fun decode(buf: FriendlyByteBuf) = C2SXaeroReadyPacket(
            buf.readUtf()
        )

        override fun encode(
            buf: FriendlyByteBuf,
            packet: C2SXaeroReadyPacket
        ) {
            buf.writeUtf(packet.dimension)
        }
    }

    override val encode = Companion::encode
    override val payloadType = Companion.payloadType
}

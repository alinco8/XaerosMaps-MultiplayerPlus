package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec

data class C2SHandshake(
    val enableWorldMapSync: Boolean,
    val downloadWindow: Int,
) : XMMPPacket<C2SHandshake>(Companion) {
    companion object : Type<C2SHandshake>(C2SHandshake::class.java) {
        override fun id() = packetId("c2s_handshake")

        override val codec = with(XMMPStreamCodec) {
            composite(
                boolean, C2SHandshake::enableWorldMapSync,
                int, C2SHandshake::downloadWindow,
                ::C2SHandshake,
            )
        }
    }
}

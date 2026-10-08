package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class C2SHandshake(
    val enableWorldMapSync: Boolean,
    val downloadWindow: Int,
) : XMMPPacket<C2SHandshake>(Companion) {
    companion object : Type<C2SHandshake>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                boolean, C2SHandshake::enableWorldMapSync,
                int, C2SHandshake::downloadWindow,
                ::C2SHandshake,
            )
        }
    }
}

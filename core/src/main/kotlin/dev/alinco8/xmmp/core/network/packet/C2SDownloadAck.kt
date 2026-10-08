package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class C2SDownloadAck(
    val completed: Int,
) : XMMPPacket<C2SDownloadAck>(Companion) {
    companion object : Type<C2SDownloadAck>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                int, C2SDownloadAck::completed,
                ::C2SDownloadAck
            )
        }
    }
}

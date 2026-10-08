package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class S2CUploadAck(val lastSeq: Long) : XMMPPacket<S2CUploadAck>(Companion) {
    companion object : Type<S2CUploadAck>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                long, S2CUploadAck::lastSeq,
                ::S2CUploadAck
            )
        }
    }
}

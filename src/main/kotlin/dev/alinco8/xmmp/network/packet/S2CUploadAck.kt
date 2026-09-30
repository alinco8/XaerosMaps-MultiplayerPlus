package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import java.util.Objects

data class S2CUploadAck(val lastSeq: Long) : XMMPPacket<S2CUploadAck>(Companion) {
    companion object : Type<S2CUploadAck>(S2CUploadAck::class.java) {
        override fun id() = packetId("s2c_upload_ack")
        override val codec = with(XMMPStreamCodec) {
            composite(
                long, S2CUploadAck::lastSeq,
                ::S2CUploadAck
            )
        }
    }

    override fun equals(other: Any?) = other is S2CUploadAck
            && lastSeq == other.lastSeq

    override fun hashCode() = Objects.hash(lastSeq)
}

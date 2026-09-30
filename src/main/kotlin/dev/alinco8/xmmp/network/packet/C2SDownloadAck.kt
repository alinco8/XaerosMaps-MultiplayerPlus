package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import java.util.Objects

data class C2SDownloadAck(val completed: Int) : XMMPPacket<C2SDownloadAck>(Companion) {
    companion object : Type<C2SDownloadAck>(C2SDownloadAck::class.java) {
        override fun id() = packetId("c2s_download_ack")
        override val codec = with(XMMPStreamCodec) {
            composite(
                int, C2SDownloadAck::completed,
                ::C2SDownloadAck
            )
        }
    }

    override fun equals(other: Any?) = other is C2SDownloadAck
            && completed == other.completed

    override fun hashCode() = Objects.hash(completed)
}

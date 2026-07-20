package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.common.XMMPPacket
import dev.alinco8.xmmp.common.XMMPPacketType
import dev.alinco8.xmmp.loc
import net.minecraft.network.FriendlyByteBuf

data class C2SChunkRowRequestPacket(
    val regionX: Int,
    val regionZ: Int,
    val row: Int,
    val missingFlags: Int,
    val dimension: String,
) : XMMPPacket<C2SChunkRowRequestPacket>() {
    companion object : XMMPPacketType<C2SChunkRowRequestPacket>() {
        override fun id() = loc("c2s_chunk_row_request")

        override fun decode(buf: FriendlyByteBuf) = C2SChunkRowRequestPacket(
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readUtf()
        )

        override fun encode(
            buf: FriendlyByteBuf,
            packet: C2SChunkRowRequestPacket,
        ) {
            buf.writeInt(packet.regionX)
            buf.writeInt(packet.regionZ)
            buf.writeInt(packet.row)
            buf.writeInt(packet.missingFlags)
            buf.writeUtf(packet.dimension)
        }
    }

    override val encode = Companion::encode
    override val payloadType = Companion.payloadType
}

package dev.alinco8.xmmp.core.server.network

import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.packet.C2SChunkUpload
import dev.alinco8.xmmp.core.network.packet.C2SDimensionSync
import dev.alinco8.xmmp.core.network.packet.C2SDownloadAck
import dev.alinco8.xmmp.core.network.packet.C2SHandshake
import dev.alinco8.xmmp.core.network.packet.C2SRegionSync
import dev.alinco8.xmmp.core.network.readId
import dev.alinco8.xmmp.core.LOGGER
import dev.alinco8.xmmp.core.server.ServerSession
import java.util.UUID
import io.netty.buffer.Unpooled

class ServerPacketHandlers(private val session: ServerSession) {
    val packets = HashMap<ResourceId, Pair<XMMPPacket.Type<*>, (XMMPPacket<*>, UUID) -> Unit>>()

    init {
        register(C2SHandshake, ::handleHandshake)
        register(C2SDimensionSync, ::handleDimensionSync)
        register(C2SRegionSync, ::handleRegionSync)
        register(C2SChunkUpload, ::handleChunkUpload)
        register(C2SDownloadAck, ::handleDownloadAck)
    }

    private fun <T : XMMPPacket<T>> register(type: XMMPPacket.Type<T>, handler: (T, UUID) -> Unit) {
        packets[type.id] = type to { packet, sender ->
            @Suppress("UNCHECKED_CAST")
            handler(packet as T, sender)
        }
    }

    fun handle(bytes: ByteArray, sender: UUID) {
        val (handler, packet) = try {
            val buf = Unpooled.wrappedBuffer(bytes)

            val id = buf.readId()
            val entry = packets[id] ?: run {
                LOGGER.warn("Unknown packet id $id from $sender")
                return
            }

            val packet = entry.first.codec.decode(buf)
            check(!buf.isReadable) { "Trailing bytes in packet $id from $sender" }

            entry.second to packet
        } catch (e: Exception) {
            LOGGER.error("Error handling packet from $sender", e)
            session.host.disconnectPlayer(sender, "Error handling packet: ${e.message}")

            return
        }

        handler(packet, sender)
    }

    fun handleHandshake(packet: C2SHandshake, sender: UUID) = session.onHandshake(sender, packet)

    fun handleDimensionSync(packet: C2SDimensionSync, sender: UUID) {
        if (session.standalone && session.host.getDimension(sender) != packet.dimension) {
            session.host.onDimensionChanged(sender, packet.dimension)
            session.onPlayerChangedDimension(sender)
        }

        val dimension = session.host.getDimension(sender) ?: return

        if (packet.dimension != dimension) {
            LOGGER.warn(
                "Player {} sent dimension sync for dimension {} but is in {}, ignoring",
                sender,
                packet.dimension,
                dimension
            )
            return
        }

        session.onDimensionSync(sender, packet.syncId)
    }

    fun handleRegionSync(
        packet: C2SRegionSync,
        sender: UUID,
    ) {
        val dimension = session.host.getDimension(sender) ?: return
        if (packet.dimension != dimension) {
            LOGGER.warn(
                "Player {} sent region sync for dimension {} but is in {}, ignoring",
                sender,
                packet.dimension,
                dimension
            )
            return
        }

        session.onRegionSync(sender, packet.layer, packet.regionPos, packet.cursor)
    }

    fun handleChunkUpload(
        packet: C2SChunkUpload,
        sender: UUID,
    ) = session.onChunkUpload(sender, packet)

    fun handleDownloadAck(
        packet: C2SDownloadAck,
        sender: UUID,
    ) = session.onDownloadAck(sender, packet.completed)
}

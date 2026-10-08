package dev.alinco8.xmmp.client.network

import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.client.ClientSession
import dev.alinco8.xmmp.client.WorldContext
import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.packet.S2CChunkData
import dev.alinco8.xmmp.core.network.packet.S2CHandshake
import dev.alinco8.xmmp.core.network.packet.S2CRegionIndex
import dev.alinco8.xmmp.core.network.packet.S2CRegionSyncDone
import dev.alinco8.xmmp.core.network.packet.S2CUploadAck
import dev.alinco8.xmmp.core.network.readId
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object ClientPacketHandlers {
    val packets = HashMap<ResourceId, Pair<XMMPPacket.Type<*>, (XMMPPacket<*>) -> Unit>>()

    init {
        register(S2CHandshake, ::handleHandshake)
        register(S2CRegionIndex, ::handleRegionIndex)
        register(S2CChunkData, ::handleChunkData)
        register(S2CRegionSyncDone, ::handleRegionSyncDone)
        register(S2CUploadAck, ::handleUploadAck)
    }

    private fun <T : XMMPPacket<T>> register(type: XMMPPacket.Type<T>, handler: (T) -> Unit) {
        packets[type.id] = type to { packet ->
            @Suppress("UNCHECKED_CAST")
            handler(packet as T)
        }
    }

    fun handle(buf: ByteBuf, isCurrentConnection: () -> Boolean = { true }) {
        val (handler, packet) = try {
            val id = buf.readId()
            val entry = packets[id] ?: run {
                LOGGER.warn("Unknown packet id $id")
                return
            }

            val packet = entry.first.codec.decode(buf)
            check(!buf.isReadable) { "Trailing bytes in packet $id" }

            entry.second to packet
        } catch (e: Exception) {
            LOGGER.error("Failed to handle packet", e)

            Minecraft.getInstance().execute {
                if (!isCurrentConnection()) return@execute

                Minecraft.getInstance().connection?.connection?.disconnect(
                    Component.literal("Failed to handle packet: ${e.message}")
                )
            }

            return
        }

        Minecraft.getInstance().execute {
            if (!isCurrentConnection()) return@execute

            handler(packet)
        }
    }

    fun handleHandshake(packet: S2CHandshake) {
        check(XMMPClient.session == null) { "Session should be null when receiving handshake" }
        with(packet) {
            XMMPClient.session = ClientSession(
                WorldContext(
                    sharedConfig,
                    XMMPClient.packetSender ?: return
                ),
            )
        }

        XMMPClient.session?.onHandshake()
    }

    fun handleRegionIndex(packet: S2CRegionIndex) {
        with(packet) {
            XMMPClient.session?.onRegionIndex(
                dimension,
                layer,
                regionRevisions
            )
        }
    }

    fun handleChunkData(packet: S2CChunkData) {
        with(packet) {
            XMMPClient.session?.onChunkData(
                dimension,
                layer,
                chunkPos,
                revision,
                payload
            )
        }
    }

    fun handleRegionSyncDone(packet: S2CRegionSyncDone) {
        with(packet) {
            XMMPClient.session?.onRegionSyncDone(
                dimension,
                layer,
                regionPos,
                revision,
                syncId
            )
        }
    }

    fun handleUploadAck(packet: S2CUploadAck) {
        with(packet) {
            XMMPClient.session?.onUploadAck(
                lastSeq
            )
        }
    }
}

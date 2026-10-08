package dev.alinco8.xmmp.server

import dev.alinco8.xmmp.core.common.ServerHost
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.id
import dev.alinco8.xmmp.io.TilePayloadCodec
import dev.alinco8.xmmp.server.network.ServerPacketSender
import io.netty.buffer.Unpooled
import java.util.UUID
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

class ServerHostImpl(private val server: MinecraftServer) : ServerHost {
    override fun <T : XMMPPacket<T>> sendToPlayer(
        playerId: UUID,
        packet: T,
    ) = server.playerList.getPlayer(playerId)?.let {
        ServerPacketSender.sendToPlayer(it, packet)
    } ?: false

    override suspend fun validateChunkBytes(chunkBytes: ByteArray): Boolean {
        return try {
            TilePayloadCodec.decode(Unpooled.wrappedBuffer(chunkBytes)) != null
        } catch (_: Exception) {
            false
        }
    }

    override fun listPlayers() = server.playerList.players.map { it.uuid }

    override fun getDimension(playerId: UUID) =
        server.playerList.getPlayer(playerId)?.level()?.dimension()?.id()

    override fun disconnectPlayer(playerId: UUID, reason: String) {
        server.playerList.getPlayer(playerId)?.connection?.disconnect(
            Component.literal(
                reason
            )
        )
    }
}

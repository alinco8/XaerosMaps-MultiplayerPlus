package dev.alinco8.xmmp.standalone

import dev.alinco8.xmmp.core.LOGGER
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.common.ServerHost
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.writeId
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StandaloneServerHost : ServerHost {
    class Connection(val channel: Channel) {
        @Volatile
        var dimension: ResourceId? = null
    }

    private val connections = ConcurrentHashMap<UUID, Connection>()

    fun register(playerId: UUID, ch: Channel) {
        LOGGER.info("Registering connection: {}", playerId)

        connections[playerId] = Connection(ch)
    }

    fun unregister(playerId: UUID) {
        LOGGER.info("Unregistering connection: {}", playerId)

        connections.remove(playerId)
    }

    override fun onDimensionChanged(playerId: UUID, dimension: ResourceId) {
        LOGGER.info("Player {} changed dimension to {}", playerId, dimension)

        connections[playerId]?.dimension = dimension
    }

    override fun <T : XMMPPacket<T>> sendToPlayer(
        playerId: UUID,
        packet: T,
    ): Boolean {
        LOGGER.info("Sending packet {} to player {}", packet.type.id, playerId)

        val ch = connections[playerId] ?: return false
        val frame = ch.channel.alloc().buffer()
        frame.writeId(packet.type.id)
        packet.type.codec.encode(frame, packet)

        ch.channel.writeAndFlush(frame)
            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)

        return true
    }

    override suspend fun validateChunkBytes(chunkBytes: ByteArray) = true

    override fun listPlayers() = connections.keys.toList()

    override fun getDimension(playerId: UUID) = connections[playerId]?.dimension

    override fun disconnectPlayer(playerId: UUID, reason: String) {
        LOGGER.info("Disconnecting player {}: {}", playerId, reason)

        connections.remove(playerId)?.channel?.close()
    }
}

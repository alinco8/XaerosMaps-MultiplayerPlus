package dev.alinco8.xmmp.plugin

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.common.ServerHost
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.readByteArray
import dev.alinco8.xmmp.core.network.writeByteArray
import dev.alinco8.xmmp.core.network.writeId
import dev.alinco8.xmmp.core.network.writeUtf
import dev.alinco8.xmmp.core.server.ServerSession
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import net.kyori.adventure.text.Component
import org.bukkit.event.Event
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRegisterChannelEvent
import org.bukkit.plugin.messaging.Messenger

class XMMPPlugin : JavaPlugin(), ServerHost {
    var session: ServerSession? = null

    companion object {
        private val noop = object : Listener {}
        const val CHANNEL = "xmmp:frame_v3"
    }

    override fun onEnable() {
        check(session == null) { "Session should be null when server starts" }
        session = ServerSession(
            dataFolder.resolve("xmmp-server.toml"),
            dataFolder.resolve("xmmp").toPath(),
            this,
        )

        registerEvent<ServerTickEndEvent> {
            session?.onTickPost()
        }
        registerEvent<PlayerJoinEvent> {
            session?.onPlayerJoin(it.player.uniqueId)
        }
        registerEvent<PlayerQuitEvent> {
            session?.onPlayerLeave(it.player.uniqueId)
        }
        registerEvent<PlayerRegisterChannelEvent> {
            if (it.channel == CHANNEL) session?.onPlayerChannelsReady(it.player.uniqueId)
        }
        registerEvent<PlayerChangedWorldEvent> {
            session?.onPlayerChangedDimension(it.player.uniqueId)
        }

        server.messenger.registerOutgoingPluginChannel(this, CHANNEL)
        server.messenger.registerIncomingPluginChannel(this, CHANNEL) { _, player, bytes ->
            val buf = Unpooled.wrappedBuffer(bytes)
            try {
                val frame = buf.readByteArray()
                check(!buf.isReadable) { "Trailing bytes in frame from ${player.uniqueId}" }

                session?.handlers?.handle(frame, player.uniqueId)
            } finally {
                buf.release()
            }
        }
    }

    override fun onDisable() {
        val s = session
        session = null
        s?.close()
    }

    private inline fun <reified E : Event> registerEvent(crossinline callback: (event: E) -> Unit) {
        server.pluginManager.registerEvent(
            E::class.java,
            noop,
            org.bukkit.event.EventPriority.NORMAL,
            { _, event -> if (event is E) callback(event) },
            this
        )
    }

    override fun <T : XMMPPacket<T>> sendToPlayer(
        playerId: UUID,
        packet: T,
    ): Boolean {
        val player = server.getPlayer(playerId) ?: return false
        if (CHANNEL !in player.listeningPluginChannels) return false

        val frame = Unpooled.buffer()
        try {
            frame.writeId(packet.type.id)
            packet.type.codec.encode(frame, packet)

            val out = Unpooled.buffer()
            try {
                out.writeByteArray(ByteBufUtil.getBytes(frame))

                val bytes = ByteBufUtil.getBytes(out)
                if (bytes.size > Messenger.MAX_MESSAGE_SIZE) return false

                player.sendPluginMessage(this, CHANNEL, bytes)
                return true
            } finally {
                out.release()
            }
        } finally {
            frame.release()
        }
    }

    override suspend fun validateChunkBytes(chunkBytes: ByteArray): Boolean {
        return true
    }

    override fun listPlayers(): List<UUID> {
        return server.onlinePlayers.map { it.uniqueId }
    }

    override fun getDimension(playerId: UUID): ResourceId? {
        val player = server.getPlayer(playerId)
        val key = player?.world?.key ?: return null
        return ResourceId(key.namespace, key.key)
    }

    override fun disconnectPlayer(playerId: UUID, reason: String) {
        val player = server.getPlayer(playerId)
        player?.kick(Component.text(reason))
    }

}

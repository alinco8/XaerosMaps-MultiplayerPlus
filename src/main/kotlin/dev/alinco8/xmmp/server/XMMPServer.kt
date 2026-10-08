package dev.alinco8.xmmp.server

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.common.ServerEvents
import dev.alinco8.xmmp.core.server.ServerSession
import dev.alinco8.xmmp.utils.ModPaths
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.LevelResource

object XMMPServer {
    @Volatile
    var session: ServerSession? = null

    fun onInitializeServer(events: ServerEvents) {
        with(events) {
            registerServerStarted(::onServerStarted)
            registerServerStopped(::onServerStopping)
            registerTickPost(::onTickPost)
            registerPlayerChannelsReady(::onPlayerChannelsReady)

            registerPlayerJoin(::onPlayerJoin)
            registerPlayerChangedDimension(::onPlayerChangedDimension)
            registerPlayerLeave(::onPlayerLeave)
        }
    }

    fun onServerStarted(server: MinecraftServer) {
        check(session == null) { "Session should be null when server starts" }

        session = ServerSession(
            ModPaths.configDir().resolve("${XMMP.MOD_ID}-server.toml").toFile(),
            server.getWorldPath(LevelResource("xmmp")),
            ServerHostImpl(server),
        )
    }

    fun onServerStopping() {
        val s = session
        session = null
        s?.close()
    }

    fun onTickPost() = session?.onTickPost()
    fun onPlayerChannelsReady(player: ServerPlayer) = session?.onPlayerChannelsReady(player.uuid)

    fun onPlayerJoin(player: ServerPlayer) = session?.onPlayerJoin(player.uuid)
    fun onPlayerLeave(player: ServerPlayer) = session?.onPlayerLeave(player.uuid)
    fun onPlayerChangedDimension(player: ServerPlayer) =
        session?.onPlayerChangedDimension(player.uuid)
}

package dev.alinco8.xmmp.server

import dev.alinco8.xmmp.common.ServerEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

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

        session = ServerSession(server)
    }

    fun onServerStopping() {
        val s = session
        session = null
        s?.close()
    }

    fun onTickPost() = session?.onTickPost()
    fun onPlayerChannelsReady(player: ServerPlayer) = session?.onPlayerChannelsReady(player)

    fun onPlayerJoin(player: ServerPlayer) = session?.onPlayerJoin(player)
    fun onPlayerLeave(player: ServerPlayer) = session?.onPlayerLeave(player)
    fun onPlayerChangedDimension(player: ServerPlayer) = session?.onPlayerChangedDimension(player)
}

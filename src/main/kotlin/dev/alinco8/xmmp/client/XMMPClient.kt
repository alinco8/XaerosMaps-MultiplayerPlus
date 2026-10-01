package dev.alinco8.xmmp.client

import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.client.xaero.XaeroController.snapshot
import dev.alinco8.xmmp.common.ClientEvents
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import xaero.map.region.MapTile

object XMMPClient {
    @Volatile
    var session: ClientSession? = null

    fun onInitializeClient(events: ClientEvents) {
        LOGGER.debug("Initializing XMMP client...")

        with(events) {
            registerWorldJoin(::onJoinWorld)
            registerTickPost(::onTickPost)
            registerWorldLeave(::onLeaveWorld)
        }
    }

    fun onJoinWorld() {
        check(session == null) { "Session should be null when joining a world" }
    }

    fun onLeaveWorld() {
        val s = session
        session = null
        s?.close()
    }

    fun onTickPost() = session?.onTickPost()

    @JvmStatic
    fun onTileWritten(dimension: ResourceKey<Level>, layer: Int, x: Int, z: Int, mapTile: MapTile) {
        session?.onTileWritten(dimension, layer, x, z, mapTile.snapshot() ?: return)
    }
}

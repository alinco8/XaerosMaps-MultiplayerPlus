//? if neoforge {
package dev.alinco8.xmmp.platform.neoforge

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.XMMPClient
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge

@Mod(value = XMMP.MOD_ID, dist = [Dist.CLIENT])
class NeoForgeEntrypointClient {
    init {
        XMMPClient.onInitializeClient()

        NeoForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onTickEnd(
        @Suppress("UNUSED_PARAMETER") e: ClientTickEvent.Post
    ) {
        XMMPClient.onTickEnd()
    }

    @SubscribeEvent
    fun onLeaveWorld(
        @Suppress("UNUSED_PARAMETER") e: ClientPlayerNetworkEvent.LoggingOut
    ) {
        XMMPClient.onLeaveWorld()
    }
}
//? }

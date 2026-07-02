//? if forge {
/*package dev.alinco8.xmmp.platform.forge


import dev.alinco8.xmmp.XMMPClient
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

class ForgeEntrypointClient {
    init {
        XMMPClient.onInitializeClient()

        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onTickEnd(
        e: TickEvent.ClientTickEvent
    ) {
        if (e.phase != TickEvent.Phase.END) return

        XMMPClient.onTickEnd()
    }

    @SubscribeEvent
    fun onLeaveWorld(
        @Suppress("UNUSED_PARAMETER") e: ClientPlayerNetworkEvent.LoggingOut
    ) {
        XMMPClient.onLeaveWorld()
    }
}
*///? }

//? if forge {
/*package dev.alinco8.xmmp.platform.forge


import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.config.ConfigScreen
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

class ForgeEntrypointClient {
    init {
        XMMPClient.onInitializeClient()

        LOADING_CONTEXT.container.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory::class.java, {
                ConfigScreenHandler.ConfigScreenFactory { _, parent ->
                    ConfigScreen.createScreen(parent)
                }
            }
        )


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

//? if forge {
/*package dev.alinco8.xmmp.platform.forge

import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.common.ClientEvents
import dev.alinco8.xmmp.config.ConfigScreen
import net.minecraft.client.player.LocalPlayer
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

class ForgeEntrypointClient : ClientEvents {

    init {
        XMMPClient.onInitializeClient(this)

        LOADING_CONTEXT.container.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory::class.java, {
                ConfigScreenHandler.ConfigScreenFactory { _, parent ->
                    ConfigScreen.createScreen(parent)
                }
            }
        )
    }

    override fun registerWorldJoin(callback: () -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<ClientPlayerNetworkEvent.LoggingIn> { _ ->
            callback()
        }
    }

    override fun registerWorldLeave(callback: () -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<ClientPlayerNetworkEvent.LoggingOut> { _ ->
            callback()
        }
    }

    override fun registerTickPost(callback: () -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<TickEvent.ClientTickEvent> { e ->
            if (e.phase != TickEvent.Phase.END) return@addListener
            callback()
        }
    }
}
*///? }

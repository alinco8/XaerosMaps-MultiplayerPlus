//? if neoforge {
package dev.alinco8.xmmp.platform.neoforge

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.common.ClientEvents
import dev.alinco8.xmmp.config.ConfigScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

@Mod(value = XMMP.MOD_ID, dist = [Dist.CLIENT])
class NeoForgeEntrypointClient(modContainer: ModContainer) : ClientEvents {
    init {
        XMMPClient.onInitializeClient(this)

        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { _, parent ->
                ConfigScreen.createScreen(parent)
            }
        )
    }

    override fun registerWorldJoin(callback: () -> Unit) {
        NeoForge.EVENT_BUS.addListener<ClientPlayerNetworkEvent.LoggingIn> {
            callback()
        }
    }

    override fun registerWorldLeave(callback: () -> Unit) {
        NeoForge.EVENT_BUS.addListener<ClientPlayerNetworkEvent.LoggingOut> {
            callback()
        }
    }

    override fun registerTickPost(callback: () -> Unit) {
        NeoForge.EVENT_BUS.addListener<ClientTickEvent.Post> {
            callback()
        }
    }
}

//? }

//? if neoforge {
package dev.alinco8.xmmp.platform.neoforge

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.config.ConfigScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

@Mod(value = XMMP.MOD_ID, dist = [Dist.CLIENT])
class NeoForgeEntrypointClient(modContainer: ModContainer) {
    init {
        XMMPClient.onInitializeClient()

        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { _, parent ->
                ConfigScreen.createScreen(parent)
            }
        )

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

//? if neoforge {
package dev.alinco8.xmmp.platform.neoforge

//? if >=1.21.8 {
/*import dev.alinco8.xmmp.packet.ChunkDataPacket
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent
*///? }

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.config.ConfigScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

@Mod(value = XMMP.MOD_ID, dist = [Dist.CLIENT])
class NeoForgeEntrypointClient(modContainer: ModContainer, modEventBus: IEventBus) {
    init {
        XMMPClient.onInitializeClient()

        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { _, parent ->
                ConfigScreen.createScreen(parent)
            }
        )

        modEventBus.register(ClientModEvents)
        NeoForge.EVENT_BUS.register(this)
    }

    object ClientModEvents {
        //? if >=1.21.8 {
        /*@SubscribeEvent
        fun onRegisterPayloadHandlers(e: RegisterClientPayloadHandlersEvent) {
            e.register(
                ChunkDataPacket.payloadType
            ) { packet, _ ->
                XMMPClient.handleChunkDataPacket(packet)
            }
        }
        *///? }
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

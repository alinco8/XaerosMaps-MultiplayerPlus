//? if fabric {
/*package dev.alinco8.xmmp.platform.fabric

import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.common.ClientEvents
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

class FabricEntrypointClient : ClientModInitializer, ClientEvents {
    override fun onInitializeClient() = XMMPClient.onInitializeClient(this)

    override fun registerWorldJoin(callback: () -> Unit) =
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            callback()
        }

    override fun registerWorldLeave(callback: () -> Unit) =
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            callback()
        }

    override fun registerTickPost(callback: () -> Unit) =
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            callback()
        }
}
*///? }

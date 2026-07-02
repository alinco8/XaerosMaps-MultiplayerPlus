//? if fabric {
/*package dev.alinco8.xmmp.platform.fabric

import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class FabricEntrypointClient : ClientModInitializer {
    override fun onInitializeClient() {
        XMMPClient.onInitializeClient()

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            XMMPClient.onTickEnd()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            XMMPClient.onLeaveWorld()
        }

        ClientPlayNetworking.registerGlobalReceiver(ChunkDataPacket.TYPE) { packet, _ ->
            XMMPClient.handleChunkDataPacket(packet)
        }
        ClientPlayNetworking.registerGlobalReceiver(S2CRegionTimestampsPacket.TYPE) { packet, _ ->
            XMMPClient.handleRegionTimestampsPacket(packet)
        }
    }

}
*///? }

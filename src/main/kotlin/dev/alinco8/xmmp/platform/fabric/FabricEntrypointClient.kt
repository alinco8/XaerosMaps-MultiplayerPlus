//? if fabric {
/*package dev.alinco8.xmmp.platform.fabric

import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.common.XMMPPacket
import dev.alinco8.xmmp.common.XMMPPacketType
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.FriendlyByteBuf

class FabricEntrypointClient : ClientModInitializer {
    override fun onInitializeClient() {
        XMMPClient.onInitializeClient()

        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            XMMPClient.onTickEnd()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            XMMPClient.onLeaveWorld()
        }

        registerClientListener(ChunkDataPacket) { packet ->
            XMMPClient.handleChunkDataPacket(packet)
        }
        registerClientListener(S2CRegionTimestampsPacket) { packet ->
            XMMPClient.handleRegionTimestampsPacket(packet)
        }
    }

    private fun <T : XMMPPacket<T>> registerClientListener(
        packetType: XMMPPacketType<T>,
        listener: (T) -> Unit
    ) {
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(packetType.payloadType) { packet, _ ->
            listener(packet)
        }
        //? } else {
        /*ClientPlayNetworking.registerGlobalReceiver(packetType.id()) { _, _, buf, _ ->
            listener(packetType.decode(buf))
        }
        *///? }
    }
}
*///? }

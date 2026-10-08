//? if fabric {
/*package dev.alinco8.xmmp.platform.fabric

import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.client.network.ClientPacketHandlers
import dev.alinco8.xmmp.common.ClientEvents
import dev.alinco8.xmmp.network.FramePacket
import io.netty.buffer.Unpooled
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.codec.StreamCodec

//? }

class FabricEntrypointClient : ClientModInitializer, ClientEvents {
    override fun onInitializeClient() {
        //? if >=1.20.5 {
        PayloadTypeRegistry
            //? if >=26 {
            /*.clientboundPlay()
            *///? } else {
            .playS2C()
            //? }
            .register(
                FramePacket.payloadType,
                StreamCodec.of(FramePacket.CODEC::encode, FramePacket.CODEC::decode)
            )
        //? }

        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(FramePacket.payloadType) { packet, _ ->
            ClientPacketHandlers.handle(Unpooled.wrappedBuffer(packet.frame))
        }
        //? } else {
        /*ClientPlayNetworking.registerGlobalReceiver(FramePacket.payloadType) { packet, _, _ ->
            ClientPacketHandlers.handle(Unpooled.wrappedBuffer(packet.frame))
        }
        *///? }

        XMMPClient.onInitializeClient(this)
    }

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

package dev.alinco8.xmmp.client.network.minecraft

//? fabric
//import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
//? neoforge && >=1.21.8
//import net.neoforged.neoforge.client.network.ClientPacketDistributor
//? neoforge && <1.21.8
import net.neoforged.neoforge.network.PacketDistributor
//? forge
//import dev.alinco8.xmmp.platform.forge.ForgeEntrypoint

import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.client.network.ClientPacketSender
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.use
import dev.alinco8.xmmp.core.network.writeId
import dev.alinco8.xmmp.network.FramePacket
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class MinecraftPacketSender : ClientPacketSender {
    override fun <T : XMMPPacket<T>> send(packet: T) {
        Unpooled.buffer().use { buf ->
            runCatching {
                buf.writeId(packet.type.id)
                packet.type.codec.encode(buf, packet)
            }.onFailure { err ->
                LOGGER.error("Failed to encode packet of type ${packet.type.id}", err)
                Minecraft.getInstance().connection?.connection?.disconnect(
                    Component.literal("Failed to encode packet of type ${packet.type.id}")
                )

                return@use
            }

            val framePacket = FramePacket(ByteBufUtil.getBytes(buf))

            //? fabric
            //ClientPlayNetworking.send(framePacket)

            //? neoforge && >=1.21.8
            //ClientPacketDistributor.sendToServer(framePacket)
            //? neoforge && <1.21.8
            PacketDistributor.sendToServer(framePacket)

            //? forge
            //ForgeEntrypoint.CHANNEL.sendToServer(framePacket)
        }
    }

    override fun close() {}
}

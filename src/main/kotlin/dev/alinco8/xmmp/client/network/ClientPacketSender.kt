package dev.alinco8.xmmp.client.network

//? fabric
//import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

//? forge
//import dev.alinco8.xmmp.platform.forge.ForgeEntrypoint

import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.client.network.minecraft.MinecraftPacketSender
import dev.alinco8.xmmp.client.network.standalone.StandalonePacketSender
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.core.LOGGER
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.network.FramePacket
import net.minecraft.client.Minecraft

interface ClientPacketSender : AutoCloseable {
    companion object {
        fun create(): ClientPacketSender? {
            val canSend = run {
                //? fabric
                //ClientPlayNetworking.canSend(FramePacket.ID)
                //? neoforge
                Minecraft.getInstance().connection?.hasChannel(FramePacket.ID) ?: false
                //? if forge {
                /*Minecraft.getInstance().connection?.let {
                    ForgeEntrypoint.CHANNEL.isRemotePresent(it.connection)
                } ?: false
                *///? }
            }
            LOGGER.debug("Can send packets to the server?: $canSend")

            if (canSend) return MinecraftPacketSender()

            val worldId = XMMPClient.getWorldId() ?: run {
                LOGGER.debug("Failed to get world ID, cannot create packet sender")

                return null
            }
            val worldConfig = XMMPConfig.HANDLER.instance().worlds[worldId] ?: run {
                LOGGER.debug("No config found for world $worldId, cannot create packet sender")

                return null
            }
            val items = worldConfig.serverAddress.split(':', limit = 2)

            try {
                val host = items[0]
                val port = items.getOrNull(1)?.toIntOrNull() ?: 25580

                val sender =
                    StandalonePacketSender(host, port)

                return sender
            } catch (e: Exception) {
                LOGGER.error(
                    "Failed to create standalone packet sender for world $worldId: ${e.message}",
                    e
                )

                return null
            }
        }
    }

    fun <T : XMMPPacket<T>> send(packet: T)
}

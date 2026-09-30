package dev.alinco8.xmmp.client.network

//? if fabric {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

*///? } else if neoforge {
import net.neoforged.neoforge.network.PacketDistributor

//? if >=1.21.8 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor

*///? }

//? } else if forge {
/*import dev.alinco8.xmmp.platform.forge.ForgeEntrypoint

*///? }

import dev.alinco8.xmmp.network.XMMPPacket

object ClientPacketSender {
    fun <T : XMMPPacket<T>> sendToServer(packet: T) {
        //? if fabric {
        /*ClientPlayNetworking.send(packet)

        *///? } else if neoforge {
        //? if >=1.21.8 {
        /*ClientPacketDistributor.sendToServer(packet)
        *///? } else {
        PacketDistributor.sendToServer(packet)
        //? }

        //? } else if forge {
        /*ForgeEntrypoint.CHANNEL.sendToServer(packet)
        *///? }
    }
}

package dev.alinco8.xmmp

import dev.alinco8.xmmp.common.XMMPPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

//? if fabric {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

*///? } else if neoforge {
//? if >=1.21.8 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor
*///? }

import net.neoforged.neoforge.network.PacketDistributor

//? } else if forge {
/*import dev.alinco8.xmmp.platform.forge.ForgeEntrypoint
import net.minecraftforge.network.PacketDistributor

*///? }

//? if >=1.20.5 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

//?}

internal object NetworkUtils {
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

    fun <T : XMMPPacket<T>> sendToPlayer(
        player: ServerPlayer,
        packet: T
    ) {
        //? if fabric {
        /*ServerPlayNetworking.send(player, packet)

        *///? } else if neoforge {
        PacketDistributor.sendToPlayer(player, packet)

        //? } else if forge {
        /*ForgeEntrypoint.CHANNEL.send(PacketDistributor.PLAYER.with({ player }), packet)

        *///? }
    }

    fun <T : XMMPPacket<T>> sendToPlayersInDimension(
        level: ServerLevel,
        packet: T,
        sender: ServerPlayer
    ) {
        //? if fabric {
        /*PlayerLookup
            //? if >=26 {
            /*.level(level)
            *///? } else {
            .world(level)
            //? }
            .forEach { player ->
                if (player != sender) ServerPlayNetworking.send(player, packet)
            }

        *///? } else if neoforge || forge {
        level.players().forEach { if (it != sender) sendToPlayer(it, packet) }

        //? }
    }
}

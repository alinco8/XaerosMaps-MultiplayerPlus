package dev.alinco8.xmmp

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

//? } else if neoforge {
/*import net.neoforged.neoforge.network.PacketDistributor

*///? }

internal object NetworkUtils {
    fun sendToServer(packet: CustomPacketPayload) {
        //? if fabric {
        ClientPlayNetworking.send(packet)

        //? } else if neoforge {
        /*PacketDistributor.sendToServer(packet)

        *///? }
    }

    fun sendToPlayer(player: ServerPlayer, packet: CustomPacketPayload) {
        //? if fabric {
        ServerPlayNetworking.send(player, packet)

        //? } else if neoforge {
        /*PacketDistributor.sendToPlayer(player, packet)

        *///? }
    }

    fun sendToPlayersInDimension(
        level: ServerLevel,
        packet: CustomPacketPayload,
        sender: ServerPlayer
    ) {
        //? if fabric {
        PlayerLookup.level(level).forEach { player ->
            if (player != sender) {
                ServerPlayNetworking.send(player, packet)
            }
        }

        //? } else if neoforge {
        /*level.players().forEach { if (it != sender) sendToPlayer(it, packet) }

        *///? }

    }
}

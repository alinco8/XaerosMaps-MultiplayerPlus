package dev.alinco8.xmmp

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

//? if fabric {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

*///? } else if neoforge {
import net.neoforged.neoforge.network.PacketDistributor

//? } else if forge {
/*import dev.alinco8.xmmp.platform.forge.ForgeEntrypoint
import net.minecraftforge.network.PacketDistributor

*///? }

//? if >=1.20.5 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

//?}

internal object NetworkUtils {
    fun <T/*?>=1.20.5>>'>'*/ : CustomPacketPayload> sendToServer(packet: T) {
        //? if fabric {
        /*ClientPlayNetworking.send(packet)

        *///? } else if neoforge {
        PacketDistributor.sendToServer(packet)

        //? } else if forge {
        /*ForgeEntrypoint.CHANNEL.sendToServer(packet)
        *///? }
    }

    fun <T/*?>=1.20.5>>'>'*/ : CustomPacketPayload> sendToPlayer(
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

    fun <T/*?>=1.20.5>>'>'*/ : CustomPacketPayload> sendToPlayersInDimension(
        level: ServerLevel,
        packet: T,
        sender: ServerPlayer
    ) {
        //? if fabric {
        /*PlayerLookup.level(level).forEach { player ->
            if (player != sender) {
                ServerPlayNetworking.send(player, packet)
            }
        }

        *///? } else if neoforge {
        level.players().forEach { if (it != sender) sendToPlayer(it, packet) }

        //? } else if forge {
        /*level.players().forEach { if (it != sender) sendToPlayer(it, packet) }

        *///? }

    }
}

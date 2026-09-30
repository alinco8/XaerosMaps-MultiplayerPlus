package dev.alinco8.xmmp.server.network

import dev.alinco8.xmmp.network.XMMPPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

//? if fabric {
/*import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

*///? } else if neoforge {
import net.neoforged.neoforge.network.PacketDistributor

//? } else if forge {
/*import dev.alinco8.xmmp.platform.forge.ForgeEntrypoint
import net.minecraftforge.network.PacketDistributor

*///? }

object ServerPacketSender {
    fun <T : XMMPPacket<T>> sendToPlayer(
        player: ServerPlayer,
        packet: T,
    ): Boolean {
        //? if fabric {
        /*if (!ServerPlayNetworking.canSend(player, packet.type.payloadType)) return false
        ServerPlayNetworking.send(player, packet)

        *///? } else if neoforge {
        if (!player.connection.hasChannel(packet)) return false
        PacketDistributor.sendToPlayer(player, packet)

        //? } else if forge {
        /*ForgeEntrypoint.CHANNEL.send(PacketDistributor.PLAYER.with({ player }), packet)

        *///? }

        return true
    }

    fun <T : XMMPPacket<T>> sendToPlayersInDimension(
        level: ServerLevel,
        packet: T,
        filter: (ServerPlayer) -> Boolean = { true },
    ): Int {
        var i = 0

        level.players().forEach {
            if (filter(it) && sendToPlayer(it, packet)) i++
        }

        return i
    }
}

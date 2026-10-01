package dev.alinco8.xmmp.server.network

import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.network.packet.C2SChunkUpload
import dev.alinco8.xmmp.network.packet.C2SDimensionSync
import dev.alinco8.xmmp.network.packet.C2SDownloadAck
import dev.alinco8.xmmp.network.packet.C2SHandshake
import dev.alinco8.xmmp.network.packet.C2SRegionSync
import dev.alinco8.xmmp.server.XMMPServer
import net.minecraft.server.level.ServerPlayer

object ServerPacketHandlers {
    fun handleHandshake(packet: C2SHandshake, player: ServerPlayer) {
        XMMPServer.session?.onHandshake(player, packet)
    }

    fun handleDimensionSync(packet: C2SDimensionSync, player: ServerPlayer) {
        if (packet.dimension != player.level().dimension()) {
            LOGGER.warn(
                "Player {} sent DimensionSyncRequest for dimension {} but is in {}, ignoring",
                player.uuid,
                packet.dimension,
                player.level().dimension()
            )
            return
        }

        XMMPServer.session?.onDimensionSync(player, packet.syncId)
    }

    fun handleRegionSync(
        packet: C2SRegionSync,
        sender: ServerPlayer,
    ) {
        if (packet.dimension != sender.level().dimension()) {
            LOGGER.warn(
                "Player {} sent region sync for dimension {} but is in {}, ignoring",
                sender.uuid,
                packet.dimension,
                sender.level().dimension()
            )
            return
        }

        XMMPServer.session?.onRegionSync(sender, packet.layer, packet.regionPos, packet.cursor)
    }

    fun handleChunkUpload(
        packet: C2SChunkUpload,
        sender: ServerPlayer,
    ) = XMMPServer.session?.onChunkUpload(sender, packet)

    fun handleDownloadAck(
        packet: C2SDownloadAck,
        sender: ServerPlayer,
    ) = XMMPServer.session?.onDownloadAck(sender, packet.completed)
}

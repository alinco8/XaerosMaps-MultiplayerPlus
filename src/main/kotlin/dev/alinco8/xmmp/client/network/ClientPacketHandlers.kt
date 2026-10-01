package dev.alinco8.xmmp.client.network

import dev.alinco8.xmmp.client.ClientSession
import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.network.packet.S2CChunkData
import dev.alinco8.xmmp.network.packet.S2CHandshake
import dev.alinco8.xmmp.network.packet.S2CRegionIndex
import dev.alinco8.xmmp.network.packet.S2CRegionSyncDone
import dev.alinco8.xmmp.network.packet.S2CUploadAck

object ClientPacketHandlers {
    fun handleHandshake(packet: S2CHandshake) {
        check(XMMPClient.session == null) { "Session should be null when receiving handshake" }
        XMMPClient.session = ClientSession(packet.serverConfig)

        XMMPClient.session?.onHandshake()
    }

    fun handleRegionIndex(packet: S2CRegionIndex) {
        packet.apply {
            XMMPClient.session?.onRegionIndex(
                dimension,
                layer,
                regionRevisions
            )
        }
    }

    fun handleChunkData(packet: S2CChunkData) {
        packet.apply {
            XMMPClient.session?.onChunkData(
                dimension,
                layer,
                chunkPos,
                revision,
                payload
            )
        }
    }

    fun handleRegionSyncDone(packet: S2CRegionSyncDone) {
        packet.apply {
            XMMPClient.session?.onRegionSyncDone(
                dimension,
                layer,
                regionPos,
                revision,
                syncId
            )
        }
    }

    fun handleUploadAck(packet: S2CUploadAck) {
        XMMPClient.session?.onUploadAck(packet.lastSeq)
    }
}

package dev.alinco8.xmmp

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier

*///? } else {
import net.minecraft.resources.ResourceLocation as Identifier

//? }

import dev.alinco8.xmmp.client.network.ClientPacketHandlers
import dev.alinco8.xmmp.common.CommonEvents
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.network.packet.C2SChunkUpload
import dev.alinco8.xmmp.network.packet.C2SDimensionSync
import dev.alinco8.xmmp.network.packet.C2SDownloadAck
import dev.alinco8.xmmp.network.packet.S2CChunkData
import dev.alinco8.xmmp.network.packet.C2SHandshake
import dev.alinco8.xmmp.network.packet.C2SRegionSync
import dev.alinco8.xmmp.network.packet.S2CHandshake
import dev.alinco8.xmmp.network.packet.S2CRegionIndex
import dev.alinco8.xmmp.network.packet.S2CRegionSyncDone
import dev.alinco8.xmmp.network.packet.S2CUploadAck
import dev.alinco8.xmmp.server.network.ServerPacketHandlers
import net.minecraft.resources.ResourceKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object XMMP {
    const val MOD_ID = "xmmp"
    const val PACKET_VERSION = "3"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    fun loc(path: String): Identifier {
        //? if forge || >=1.21 {
        return Identifier.fromNamespaceAndPath(MOD_ID, path)
        //? } else {
        /*return Identifier(MOD_ID, path)
        *///? }
    }

    fun onInitialize(events: CommonEvents) {
        LOGGER.debug("Initializing XMMP")

        XMMPConfig.HANDLER.load()

        with(events) {
            registerPackets(PACKET_VERSION, ::onRegisterPackets)
        }
    }

    fun onRegisterPackets(registry: CommonEvents.PacketRegistry) = with(registry) {
        registerToServer(C2SChunkUpload, ServerPacketHandlers::handleChunkUpload)
        registerToServer(C2SDimensionSync, ServerPacketHandlers::handleDimensionSync)
        registerToServer(C2SDownloadAck, ServerPacketHandlers::handleDownloadAck)
        registerToServer(C2SHandshake, ServerPacketHandlers::handleHandshake)
        registerToServer(C2SRegionSync, ServerPacketHandlers::handleRegionSync)

        registerToClient(S2CChunkData) {
            ClientPacketHandlers.handleChunkData(it)
        }
        registerToClient(S2CHandshake) {
            ClientPacketHandlers.handleHandshake(it)
        }
        registerToClient(S2CRegionIndex) {
            ClientPacketHandlers.handleRegionIndex(it)
        }
        registerToClient(S2CRegionSyncDone) {
            ClientPacketHandlers.handleRegionSyncDone(it)
        }
        registerToClient(S2CUploadAck) {
            ClientPacketHandlers.handleUploadAck(it)
        }
    }
}

internal fun ResourceKey<*>.id(): Identifier {
    //? if >=1.21.11 {
    /*return this.identifier()
    *///? } else {
    return this.location()
    //? }
}

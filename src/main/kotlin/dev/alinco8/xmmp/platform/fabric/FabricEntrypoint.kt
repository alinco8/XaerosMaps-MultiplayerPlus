//? if fabric {
/*package dev.alinco8.xmmp.platform.fabric

//? if >=26 {
/*import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
*///? } else {
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents as ServerEntityLevelChangeEvents
//? }

//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
//? }

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.common.XMMPPacket
import dev.alinco8.xmmp.common.XMMPPacketType
import dev.alinco8.xmmp.packet.C2SChunkRowRequestPacket
import dev.alinco8.xmmp.packet.C2SXaeroReadyPacket
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer

class FabricEntrypoint : ModInitializer {
    override fun onInitialize() {
        XMMP.onInitialize()

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            XMMP.onServerStarted(server)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
            XMMP.onServerStopping()
        }
        ServerPlayConnectionEvents.DISCONNECT.register { listener, _ ->
            XMMP.onPlayerLeave(listener.player)
        }
        ServerEntityLevelChangeEvents
            //? if >=26 {
            /*.AFTER_PLAYER_CHANGE_LEVEL
            *///? } else {
            .AFTER_PLAYER_CHANGE_WORLD
            //? }
            .register { player, _, _ ->
                XMMP.onPlayerDimensionChange(player)
            }

        //? if >=1.20.5 {
        registerPacket(C2SChunkRowRequestPacket)
        registerPacket(C2SXaeroReadyPacket)
        registerPacket(ChunkDataPacket)
        registerPacket(S2CRegionTimestampsPacket)
        //? }

        registerCommonListener(C2SChunkRowRequestPacket) { packet, player ->
            XMMP.handleChunkRowRequestPacket(packet, player)
        }
        registerCommonListener(C2SXaeroReadyPacket) { packet, player ->
            XMMP.handleXaeroReadyPacket(packet, player)
        }
        registerCommonListener(ChunkDataPacket) { packet, player ->
            XMMP.handleChunkDataPacket(packet, player)
        }
    }

    private fun <T : XMMPPacket<T>> registerCommonListener(
        packetType: XMMPPacketType<T>,
        listener: (T, ServerPlayer) -> Unit
    ) {
        //? if >=1.20.5 {
        ServerPlayNetworking.registerGlobalReceiver(packetType.payloadType) { packet, ctx ->
            listener(packet, ctx.player())
        }
        //? } else {
        /*ServerPlayNetworking.registerGlobalReceiver(packetType.id()) { _, player, _, buf, _ ->
            listener(packetType.decode(buf), player)
        }
        *///? }
    }

    //? if >=1.20.5 {
    private fun <T : XMMPPacket<T>> registerPacket(type: XMMPPacketType<T>) {
        val codec = net.minecraft.network.codec.StreamCodec
            .of(type::encode, type::decode)

        PayloadTypeRegistry
            //? if >=26 {
            /*.serverboundPlay()
            *///? } else {
            .playC2S()
            //? }
            .register(type.payloadType, codec)
        PayloadTypeRegistry
            //? if >=26 {
            /*.clientboundPlay()
            *///? } else {
            .playS2C()
            //? }
            .register(type.payloadType, codec)
    }
    //? }
}
*///? }

//? if fabric {
/*package dev.alinco8.xmmp.platform.fabric

//? if >=26 {
/*import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
*///? } else {
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents as ServerEntityLevelChangeEvents
//? }

//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.codec.StreamCodec
//? }

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.common.ServerEvents
import dev.alinco8.xmmp.network.FramePacket
import dev.alinco8.xmmp.server.XMMPServer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class FabricEntrypoint : ModInitializer, ServerEvents {
    override fun onInitialize() {
        //? if >=1.20.5 {
        PayloadTypeRegistry
            //? if >=26 {
            /*.serverboundPlay()
            *///? } else {
            .playC2S()
            //? }
            .register(
                FramePacket.payloadType,
                StreamCodec.of(FramePacket.CODEC::encode, FramePacket.CODEC::decode)
            )
        //? }

        //? if >=1.20.5 {
        ServerPlayNetworking.registerGlobalReceiver(FramePacket.payloadType) { packet, ctx ->
            XMMPServer.session?.handlers?.handle(packet.frame, ctx.player().uuid)
        }
        //? } else {
        /*ServerPlayNetworking.registerGlobalReceiver(FramePacket.payloadType) { packet, player, _ ->
            XMMPServer.session?.handlers?.handle(packet.frame, player.uuid)
        }
        *///? }

        XMMP.onInitialize()
        XMMPServer.onInitializeServer(this)
    }

    override fun registerServerStarted(callback: (server: MinecraftServer) -> Unit) =
        ServerLifecycleEvents.SERVER_STARTED.register(callback)

    override fun registerServerStopped(callback: () -> Unit) =
        ServerLifecycleEvents.SERVER_STOPPED.register { _ ->
            callback()
        }

    override fun registerTickPost(callback: () -> Unit) =
        ServerTickEvents.END_SERVER_TICK.register { _ ->
            callback()
        }

    override fun registerPlayerChannelsReady(callback: (player: ServerPlayer) -> Unit) {
        //? if <1.20.5 {
        /*net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents.REGISTER
            .register { listener, _, _, channels ->
                if (FramePacket.ID in channels) callback(listener.player)
            }
        *///? }
    }

    override fun registerPlayerJoin(callback: (player: ServerPlayer) -> Unit) =
        ServerPlayConnectionEvents.JOIN.register { listener, _, _ ->
            callback(listener.player)
        }

    override fun registerPlayerLeave(callback: (player: ServerPlayer) -> Unit) =
        ServerPlayConnectionEvents.DISCONNECT.register { listener, _ ->
            callback(listener.player)
        }

    override fun registerPlayerChangedDimension(callback: (player: ServerPlayer) -> Unit) =
        ServerEntityLevelChangeEvents
            //? if >=26 {
            /*.AFTER_PLAYER_CHANGE_LEVEL
            *///? } else {
            .AFTER_PLAYER_CHANGE_WORLD
            //? }
            .register { player, _, _ ->
                callback(player)
            }
}
*///? }

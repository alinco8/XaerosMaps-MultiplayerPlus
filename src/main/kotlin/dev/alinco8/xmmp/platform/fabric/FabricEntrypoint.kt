//? if fabric {
/*package dev.alinco8.xmmp.platform.fabric

//? if >=26 {
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
//? } else {
/*import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents as ServerEntityLevelChangeEvents
*///? }

//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.codec.StreamCodec
//? }

import com.mojang.brigadier.CommandDispatcher
import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.common.CommonEvents
import dev.alinco8.xmmp.common.ServerEvents
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.packet.S2CHandshake
import dev.alinco8.xmmp.server.XMMPServer
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class FabricEntrypoint : ModInitializer, CommonEvents, ServerEvents {
    override fun onInitialize() {
        XMMP.onInitialize(this)
        XMMPServer.onInitializeServer(this)
    }

    override fun registerCommands(callback: (dispatcher: CommandDispatcher<CommandSourceStack>) -> Unit) =
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> callback(dispatcher) }

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
                if (S2CHandshake.id() in channels) callback(listener.player)
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
            .AFTER_PLAYER_CHANGE_LEVEL
            //? } else {
            /*.AFTER_PLAYER_CHANGE_WORLD
            *///? }
            .register { player, _, _ ->
                callback(player)
            }

    class PacketRegistry : CommonEvents.PacketRegistry {
        val isClient = FabricLoader.getInstance().environmentType == EnvType.CLIENT

        override fun <T : XMMPPacket<T>> registerToServer(
            packetType: XMMPPacket.Type<T>,
            handler: CommonEvents.ServerPacketHandler<T>,
        ) {
            //? if >=1.20.5 {
            PayloadTypeRegistry
                //? if >=26 {
                .serverboundPlay()
                //? } else {
                /*.playC2S()
                *///? }
                .register(
                    packetType.payloadType,
                    StreamCodec.of(packetType.codec::encode, packetType.codec::decode)
                )
            //? }

            //? if >=1.20.5 {
            ServerPlayNetworking.registerGlobalReceiver(packetType.payloadType) { packet, ctx ->
                handler(packet, ctx.player())
            }
            //? } else {
            /*ServerPlayNetworking.registerGlobalReceiver(packetType.payloadType) { packet, player, _ ->
                handler(packet, player)
            }
            *///? }
        }

        override fun <T : XMMPPacket<T>> registerToClient(
            packetType: XMMPPacket.Type<T>,
            handler: CommonEvents.ClientPacketHandler<T>,
        ) {
            //? if >=1.20.5 {
            PayloadTypeRegistry
                //? if >=26 {
                .clientboundPlay()
                //? } else {
                /*.playS2C()
                *///? }
                .register(
                    packetType.payloadType,
                    StreamCodec.of(packetType.codec::encode, packetType.codec::decode)
                )
            //? }

            if (!isClient) return

            //? if >=1.20.5 {
            ClientPlayNetworking.registerGlobalReceiver(packetType.payloadType) { packet, _ ->
                handler(packet)
            }
            //? } else {
            /*ClientPlayNetworking.registerGlobalReceiver(packetType.payloadType) { packet, _, _ ->
                handler(packet)
            }
            *///? }
        }

        override fun <T : XMMPPacket<T>> registerBidirectional(
            packetType: XMMPPacket.Type<T>,
            serverHandler: CommonEvents.ServerPacketHandler<T>,
            clientHandler: CommonEvents.ClientPacketHandler<T>,
        ) {
            registerToServer(packetType, serverHandler)
            registerToClient(packetType, clientHandler)
        }
    }

    override fun registerPackets(
        version: String,
        callback: (CommonEvents.PacketRegistry) -> Unit,
    ) = callback(PacketRegistry())
}
*///? }

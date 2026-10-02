//? if neoforge {
package dev.alinco8.xmmp.platform.neoforge

//? <1.21.8
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.common.CommonEvents
import dev.alinco8.xmmp.common.ServerEvents
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import dev.alinco8.xmmp.network.XMMPPacket
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import com.mojang.brigadier.CommandDispatcher
import dev.alinco8.xmmp.server.XMMPServer

@Mod(XMMP.MOD_ID)
class NeoForgeEntrypoint : CommonEvents, ServerEvents {
    init {
        XMMP.onInitialize(this)
        XMMPServer.onInitializeServer(this)
    }

    override fun registerCommands(callback: (dispatcher: CommandDispatcher<CommandSourceStack>) -> Unit) {
        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { e ->
            callback(e.dispatcher)
        }
    }

    override fun registerServerStarted(callback: (server: MinecraftServer) -> Unit) {
        NeoForge.EVENT_BUS.addListener<ServerStartedEvent> { e ->
            callback(e.server)
        }
    }

    override fun registerTickPost(callback: () -> Unit) {
        NeoForge.EVENT_BUS.addListener<ServerTickEvent.Post> { _ ->
            callback()
        }
    }

    override fun registerServerStopped(callback: () -> Unit) {
        NeoForge.EVENT_BUS.addListener<ServerStoppedEvent> {
            callback()
        }
    }

    override fun registerPlayerChannelsReady(callback: (player: ServerPlayer) -> Unit) {}

    override fun registerPlayerJoin(callback: (player: ServerPlayer) -> Unit) {
        NeoForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedInEvent> { e ->
            callback(e.entity as ServerPlayer)
        }
    }

    override fun registerPlayerChangedDimension(
        callback: (player: ServerPlayer) -> Unit,
    ) {
        NeoForge.EVENT_BUS.addListener<PlayerEvent.PlayerChangedDimensionEvent> { e ->
            callback(e.entity as ServerPlayer)
        }
    }

    override fun registerPlayerLeave(callback: (player: ServerPlayer) -> Unit) {
        NeoForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedOutEvent> { e ->
            callback(e.entity as ServerPlayer)
        }
    }

    override fun registerPackets(
        version: String,
        callback: (CommonEvents.PacketRegistry) -> Unit,
    ) {
        MOD_BUS.addListener<RegisterPayloadHandlersEvent> { e ->
            callback(PacketRegistry(e.registrar(version).optional()))
        }
    }

    class PacketRegistry(private val registrar: PayloadRegistrar) : CommonEvents.PacketRegistry {
        override fun <T : XMMPPacket<T>> registerToServer(
            packetType: XMMPPacket.Type<T>,
            handler: CommonEvents.ServerPacketHandler<T>,
        ) {
            registrar.playToServer(
                packetType.payloadType,
                StreamCodec.of(
                    packetType.codec::encode,
                    packetType.codec::decode
                )
            ) { packet, ctx ->
                handler(packet, ctx.player() as ServerPlayer)
            }
        }

        override fun <T : XMMPPacket<T>> registerToClient(
            packetType: XMMPPacket.Type<T>,
            handler: CommonEvents.ClientPacketHandler<T>,
        ) {
            registrar.playToClient(
                packetType.payloadType,
                StreamCodec.of(
                    packetType.codec::encode,
                    packetType.codec::decode
                ),
                { packet, _ ->
                    handler(packet)
                }
            )
        }

        override fun <T : XMMPPacket<T>> registerBidirectional(
            packetType: XMMPPacket.Type<T>,
            serverHandler: CommonEvents.ServerPacketHandler<T>,
            clientHandler: CommonEvents.ClientPacketHandler<T>,
        ) {
            registrar.playBidirectional(
                packetType.payloadType,
                StreamCodec.of(
                    packetType.codec::encode,
                    packetType.codec::decode
                ),
                //? if >=1.21.8 {
                /*{ packet, ctx ->
                    serverHandler(packet, ctx.player() as ServerPlayer)
                },
                { packet, _ ->
                    clientHandler(packet)
                },
                *///? } else {
                DirectionalPayloadHandler(
                    { packet, _ ->
                        clientHandler(packet)
                    },
                    { packet, ctx ->
                        serverHandler(packet, ctx.player() as ServerPlayer)
                    }
                )
                //? }
            )
        }
    }
}
//? }

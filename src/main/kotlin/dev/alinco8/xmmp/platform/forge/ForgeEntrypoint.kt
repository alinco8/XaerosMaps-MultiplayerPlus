//? if forge {
/*package dev.alinco8.xmmp.platform.forge

import com.mojang.brigadier.CommandDispatcher
import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.common.CommonEvents
import dev.alinco8.xmmp.common.ServerEvents
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.server.XMMPServer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(XMMP.MOD_ID)
class ForgeEntrypoint : CommonEvents, ServerEvents {
    init {
        XMMP.onInitialize(this)
        XMMPServer.onInitializeServer(this)

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
            Runnable {
                ForgeEntrypointClient()
            }
        }
    }

    companion object {
        lateinit var CHANNEL: SimpleChannel
    }

    override fun registerCommands(callback: (dispatcher: CommandDispatcher<CommandSourceStack>) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<RegisterCommandsEvent> {
            callback(it.dispatcher)
        }
    }

    override fun registerServerStarted(callback: (server: MinecraftServer) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<ServerStartedEvent> {
            callback(it.server)
        }
    }

    override fun registerTickPost(callback: () -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<TickEvent.ServerTickEvent> {
            if (it.phase != TickEvent.Phase.END) return@addListener
            callback()
        }
    }

    override fun registerServerStopped(callback: () -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<ServerStoppedEvent> {
            callback()
        }
    }

    override fun registerPlayerChannelsReady(callback: (player: ServerPlayer) -> Unit) {}

    override fun registerPlayerJoin(callback: (player: ServerPlayer) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedInEvent> {
            callback(it.entity as ServerPlayer)
        }
    }

    override fun registerPlayerChangedDimension(callback: (player: ServerPlayer) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<PlayerEvent.PlayerChangedDimensionEvent> {
            callback(it.entity as ServerPlayer)
        }
    }

    override fun registerPlayerLeave(callback: (player: ServerPlayer) -> Unit) {
        MinecraftForge.EVENT_BUS.addListener<PlayerEvent.PlayerLoggedOutEvent> {
            callback(it.entity as ServerPlayer)
        }
    }

    @Suppress("INFERRED_INVISIBLE_RETURN_TYPE_WARNING")
    class PacketRegistry(private val channel: SimpleChannel) :
        CommonEvents.PacketRegistry {
        var index = 0

        override fun <T : XMMPPacket<T>> registerToServer(
            packetType: XMMPPacket.Type<T>,
            handler: CommonEvents.ServerPacketHandler<T>,
        ) {
            channel.messageBuilder(
                packetType.packet,
                index++,
                NetworkDirection.PLAY_TO_SERVER
            )
                .encoder { packet, buf -> packetType.codec.encode(buf, packet) }
                .decoder { buf -> packetType.codec.decode(buf) }
                .consumerMainThread { packet, ctx ->
                    handler(packet, ctx.get().sender!!)

                    ctx.get().packetHandled = true
                }
                .add()
        }

        override fun <T : XMMPPacket<T>> registerToClient(
            packetType: XMMPPacket.Type<T>,
            handler: CommonEvents.ClientPacketHandler<T>,
        ) {
            channel.messageBuilder(
                packetType.packet,
                index++,
                NetworkDirection.PLAY_TO_CLIENT
            )
                .encoder { packet, buf -> packetType.codec.encode(buf, packet) }
                .decoder { buf -> packetType.codec.decode(buf) }
                .consumerMainThread { packet, ctx ->
                    handler(packet)

                    ctx.get().packetHandled = true
                }
                .add()
        }

        override fun <T : XMMPPacket<T>> registerBidirectional(
            packetType: XMMPPacket.Type<T>,
            serverHandler: CommonEvents.ServerPacketHandler<T>,
            clientHandler: CommonEvents.ClientPacketHandler<T>,
        ) {
            channel.messageBuilder(packetType.packet, index++)
                .encoder { packet, buf -> packetType.codec.encode(buf, packet) }
                .decoder { buf -> packetType.codec.decode(buf) }
                .consumerMainThread { packet, ctx ->
                    if (ctx.get().direction == NetworkDirection.PLAY_TO_SERVER) {
                        serverHandler(packet, ctx.get().sender!!)
                    } else {
                        clientHandler(packet)
                    }

                    ctx.get().packetHandled = true
                }
                .add()
        }
    }

    override fun registerPackets(
        version: String,
        callback: (CommonEvents.PacketRegistry) -> Unit,
    ) {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            XMMP.loc("main"),
            { version },
            NetworkRegistry.acceptMissingOr(version),
            NetworkRegistry.acceptMissingOr(version)
        )

        MOD_BUS.addListener<FMLCommonSetupEvent> {
            it.enqueueWork {
                callback(PacketRegistry(CHANNEL))
            }
        }
    }
}
*///? }

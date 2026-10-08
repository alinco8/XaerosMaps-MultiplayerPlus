//? if forge {
/*package dev.alinco8.xmmp.platform.forge

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.client.network.ClientPacketHandlers
import dev.alinco8.xmmp.common.ServerEvents
import dev.alinco8.xmmp.network.FramePacket
import dev.alinco8.xmmp.server.XMMPServer
import io.netty.buffer.Unpooled
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
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
class ForgeEntrypoint : ServerEvents {
    init {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            XMMP.loc("main"),
            { XMMP.PACKET_VERSION },
            NetworkRegistry.acceptMissingOr(XMMP.PACKET_VERSION),
            NetworkRegistry.acceptMissingOr(XMMP.PACKET_VERSION)
        )

        MOD_BUS.addListener<FMLCommonSetupEvent> {
            it.enqueueWork {
                var index = 0

                CHANNEL.messageBuilder(FramePacket::class.java, index++)
                    .encoder { packet, buf -> FramePacket.CODEC.encode(buf, packet) }
                    .decoder { buf -> FramePacket.CODEC.decode(buf) }
                    .consumerMainThread { packet, ctx ->
                        if (ctx.get().direction == NetworkDirection.PLAY_TO_SERVER) {
                            XMMPServer.session?.handlers?.handle(
                                packet.frame,
                                ctx.get().sender!!.uuid
                            )
                        } else {
                            ClientPacketHandlers.handle(Unpooled.wrappedBuffer(packet.frame))
                        }

                        ctx.get().packetHandled = true
                    }
                    .add()
            }
        }

        XMMP.onInitialize()
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
}
*///? }

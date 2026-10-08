//? if neoforge {
package dev.alinco8.xmmp.platform.neoforge

//? <1.21.8
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.client.network.ClientPacketHandlers
import dev.alinco8.xmmp.common.ServerEvents
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import dev.alinco8.xmmp.network.FramePacket
import dev.alinco8.xmmp.server.XMMPServer
import io.netty.buffer.Unpooled

@Mod(XMMP.MOD_ID)
class NeoForgeEntrypoint : ServerEvents {
    init {
        MOD_BUS.addListener(::registerPackets)

        XMMP.onInitialize()
        XMMPServer.onInitializeServer(this)
    }

    fun registerPackets(e: RegisterPayloadHandlersEvent) {
        e.registrar(XMMP.PACKET_VERSION).optional().playBidirectional(
            FramePacket.payloadType,
            StreamCodec.of(FramePacket.CODEC::encode, FramePacket.CODEC::decode),
            //? <1.21.8
            DirectionalPayloadHandler(
                { packet, _ -> ClientPacketHandlers.handle(Unpooled.wrappedBuffer(packet.frame)) },
                { packet, ctx ->
                    XMMPServer.session?.handlers?.handle(
                        packet.frame,
                        ctx.player().uuid
                    )
                }
                //? <1.21.8
            )
        )
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
}
//? }

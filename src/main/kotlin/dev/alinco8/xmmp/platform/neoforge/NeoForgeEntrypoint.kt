//? if neoforge {
package dev.alinco8.xmmp.platform.neoforge

//? if <1.21.8 {
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler
//? }

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.common.XMMPPacket
import dev.alinco8.xmmp.common.XMMPPacketType
import dev.alinco8.xmmp.packet.C2SChunkRowRequestPacket
import dev.alinco8.xmmp.packet.C2SXaeroReadyPacket
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import net.minecraft.network.codec.StreamCodec
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadHandler
import net.neoforged.neoforge.network.registration.PayloadRegistrar

@Mod(XMMP.MOD_ID)
class NeoForgeEntrypoint(modEventBus: IEventBus) {
    init {
        XMMP.onInitialize()

        modEventBus.register(ModEvents)
        NeoForge.EVENT_BUS.register(this)
    }

    object ModEvents {
        @SubscribeEvent
        fun onRegisterPayloadHandlers(e: RegisterPayloadHandlersEvent) {
            val registrar = e.registrar("1");

            //? if >=1.21.8 {
            /*registrar.registerBidirectional(
                ChunkDataPacket,
                { packet, ctx ->
                    XMMP.handleChunkDataPacket(packet, ctx.player())
                }
            )
            *///? } else {
            registrar.registerBidirectional(
                ChunkDataPacket,
                { packet, _ ->
                    XMMPClient.handleChunkDataPacket(packet)
                },
                { packet, ctx ->
                    XMMP.handleChunkDataPacket(packet, ctx.player())
                }
            )
            //? }

            registrar.registerToServer(
                C2SXaeroReadyPacket,
                { packet, ctx ->
                    XMMP.handleXaeroReadyPacket(packet, ctx.player())
                }
            )

            registrar.registerToServer(
                C2SChunkRowRequestPacket,
                { packet, ctx ->
                    XMMP.handleChunkRowRequestPacket(packet, ctx.player())
                }
            )

            registrar.registerToClient(
                S2CRegionTimestampsPacket,
                { packet, _ ->
                    XMMPClient.handleRegionTimestampsPacket(packet)
                }
            )
        }
    }

    @SubscribeEvent
    fun onServerStarted(e: ServerStartingEvent) {
        XMMP.onServerStarted(e.server)
    }

    @SubscribeEvent
    fun onServerStopped(
        @Suppress("UNUSED_PARAMETER") e: ServerStoppedEvent,
    ) {
        XMMP.onServerStopping()
    }

    @SubscribeEvent
    fun onPlayerDimensionChange(
        e: PlayerEvent.PlayerChangedDimensionEvent
    ) {
        XMMP.onPlayerDimensionChange(e.entity)
    }

    @SubscribeEvent
    fun onPlayerLeaveWorld(
        e: PlayerEvent.PlayerLoggedOutEvent
    ) {
        XMMP.onPlayerLeave(e.entity)
    }
}

//? if >=1.21.8 {
/*private fun <T : XMMPPacket<T>> PayloadRegistrar.registerBidirectional(
    packet: XMMPPacketType<T>,
    serverHandler: IPayloadHandler<T>,
) {
    this.playBidirectional(
        packet.payloadType,
        StreamCodec.of(packet::encode, packet::decode),
        serverHandler
    )
}
*///? } else {
private fun <T : XMMPPacket<T>> PayloadRegistrar.registerBidirectional(
    packet: XMMPPacketType<T>,
    clientHandler: IPayloadHandler<T>,
    serverHandler: IPayloadHandler<T>,
) {
    this.playBidirectional(
        packet.payloadType,
        StreamCodec.of(packet::encode, packet::decode),
        DirectionalPayloadHandler(clientHandler, serverHandler)
    )
}
//? }

private fun <T : XMMPPacket<T>> PayloadRegistrar.registerToServer(
    packet: XMMPPacketType<T>,
    serverHandler: IPayloadHandler<T>,
) {
    this.playToServer(
        packet.payloadType,
        StreamCodec.of(packet::encode, packet::decode),
        serverHandler
    )
}

private fun <T : XMMPPacket<T>> PayloadRegistrar.registerToClient(
    packet: XMMPPacketType<T>,
    clientHandler: IPayloadHandler<T>,
) {
    this.playToClient(
        packet.payloadType,
        StreamCodec.of(packet::encode, packet::decode),
        clientHandler
    )
}

//? }

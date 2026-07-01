//? if neoforge {
/*package dev.alinco8.xmmp.platform.neoforge

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.config.ConfigScreen
import dev.alinco8.xmmp.packet.C2SChunkRowRequestPacket
import dev.alinco8.xmmp.packet.C2SXaeroReadyPacket
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler

@Mod(XMMP.MOD_ID)
class NeoForgeEntrypoint(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        XMMP.onInitialize()

        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { _, parent ->
                ConfigScreen.createScreen(parent)
            }
        )

        modEventBus.register(ModEvents)
        NeoForge.EVENT_BUS.register(this)
    }

    object ModEvents {
        @SubscribeEvent
        fun onRegisterPayloadHandlers(e: RegisterPayloadHandlersEvent) {
            val registrar = e.registrar("1");

            registrar.playBidirectional(
                ChunkDataPacket.TYPE,
                ChunkDataPacket.STREAM_CODEC,
                DirectionalPayloadHandler({ packet, _ ->
                    XMMPClient.handleChunkDataPacket(packet)
                }, { packet, ctx ->
                    XMMP.handleChunkDataPacket(packet, ctx.player())
                })
            )

            registrar.playToServer(
                C2SXaeroReadyPacket.TYPE,
                C2SXaeroReadyPacket.STREAM_CODEC
            ) { packet, ctx ->
                XMMP.handleXaeroReadyPacket(packet, ctx.player())
            }
            registrar.playToServer(
                C2SChunkRowRequestPacket.TYPE,
                C2SChunkRowRequestPacket.STREAM_CODEC
            ) { packet, ctx ->
                XMMP.handleChunkRowRequestPacket(packet, ctx.player())
            }

            registrar.playToClient(
                S2CRegionTimestampsPacket.TYPE,
                S2CRegionTimestampsPacket.STREAM_CODEC
            ) { packet, _ ->
                XMMPClient.handleRegionTimestampsPacket(packet)
            }
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
*///? }

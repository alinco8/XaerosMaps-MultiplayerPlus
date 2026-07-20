//? if forge {
/*package dev.alinco8.xmmp.platform.forge

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.loc
import dev.alinco8.xmmp.packet.C2SChunkRowRequestPacket
import dev.alinco8.xmmp.packet.C2SXaeroReadyPacket
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel
import thedarkcolour.kotlinforforge.forge.DIST
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(XMMP.MOD_ID)
class ForgeEntrypoint {
    init {
        XMMP.onInitialize()

        MOD_BUS.register(ModEvents)
        MinecraftForge.EVENT_BUS.register(this)

        if (DIST.isClient) ForgeEntrypointClient()
    }

    companion object {
        const val PROTOCOL_VERSION = "1"

        val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
            loc("main"),
            { PROTOCOL_VERSION },
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        )

        object ModEvents {
            @SubscribeEvent
            @Suppress("INFERRED_INVISIBLE_RETURN_TYPE_WARNING")
            fun onCommonSetup(event: FMLCommonSetupEvent) {
                event.enqueueWork {
                    var id = 0

                    CHANNEL.registerMessage(
                        id++,
                        C2SXaeroReadyPacket::class.java,
                        { packet, buf -> C2SXaeroReadyPacket.encode(buf, packet) },
                        C2SXaeroReadyPacket::decode
                    ) { packet, ctx ->
                        val sender = ctx.get().sender
                        ctx.get().enqueueWork {
                            if (sender != null) {
                                XMMP.handleXaeroReadyPacket(packet, sender)
                            }
                        }
                        ctx.get().packetHandled = true
                    }

                    CHANNEL.registerMessage(
                        id++,
                        ChunkDataPacket::class.java,
                        { packet, buf -> ChunkDataPacket.encode(buf, packet) },
                        ChunkDataPacket::decode
                    ) { packet, ctx ->
                        val ctx = ctx.get()
                        ctx.enqueueWork {
                            when (ctx.direction) {
                                NetworkDirection.PLAY_TO_SERVER -> {
                                    XMMP.handleChunkDataPacket(packet, ctx.sender!!)
                                }

                                NetworkDirection.PLAY_TO_CLIENT -> {
                                    XMMPClient.handleChunkDataPacket(packet)
                                }

                                else -> {}
                            }
                        }
                        ctx.packetHandled = true
                    }

                    CHANNEL.registerMessage(
                        id++,
                        C2SChunkRowRequestPacket::class.java,
                        { packet, buf -> C2SChunkRowRequestPacket.encode(buf, packet) },
                        C2SChunkRowRequestPacket::decode
                    ) { packet, ctx ->
                        val sender = ctx.get().sender
                        ctx.get().enqueueWork {
                            if (sender != null) {
                                XMMP.handleChunkRowRequestPacket(packet, sender)
                            }
                        }
                        ctx.get().packetHandled = true
                    }

                    CHANNEL.registerMessage(
                        id++,
                        S2CRegionTimestampsPacket::class.java,
                        { packet, buf -> S2CRegionTimestampsPacket.encode(buf, packet) },
                        S2CRegionTimestampsPacket::decode
                    ) { packet, ctx ->
                        ctx.get().enqueueWork {
                            DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                                Runnable {
                                    XMMPClient.handleRegionTimestampsPacket(packet)
                                }
                            }
                        }
                        ctx.get().packetHandled = true
                    }
                }
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

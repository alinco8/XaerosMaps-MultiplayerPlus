//? if fabric {
package dev.alinco8.xmmp.platform.fabric

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.packet.C2SChunkRowRequestPacket
import dev.alinco8.xmmp.packet.C2SXaeroReadyPacket
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

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
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register { player, _, _ ->
            XMMP.onPlayerDimensionChange(player)
        }

        registerPacket(C2SChunkRowRequestPacket.TYPE, C2SChunkRowRequestPacket.STREAM_CODEC)
        registerPacket(C2SXaeroReadyPacket.TYPE, C2SXaeroReadyPacket.STREAM_CODEC)
        registerPacket(ChunkDataPacket.TYPE, ChunkDataPacket.STREAM_CODEC)
        registerPacket(S2CRegionTimestampsPacket.TYPE, S2CRegionTimestampsPacket.STREAM_CODEC)

        ServerPlayNetworking.registerGlobalReceiver(ChunkDataPacket.TYPE) { packet, ctx ->
            XMMP.handleChunkDataPacket(packet, ctx.player())
        }
        ServerPlayNetworking.registerGlobalReceiver(C2SXaeroReadyPacket.TYPE) { packet, ctx ->
            XMMP.handleXaeroReadyPacket(packet, ctx.player())
        }

        ServerPlayNetworking.registerGlobalReceiver(C2SChunkRowRequestPacket.TYPE) { packet, ctx ->
            XMMP.handleChunkRowRequestPacket(packet, ctx.player())
        }
    }

    private fun <T : CustomPacketPayload> registerPacket(
        type: CustomPacketPayload.Type<T>,
        codec: StreamCodec<FriendlyByteBuf, T>
    ) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec)
        PayloadTypeRegistry.clientboundPlay().register(type, codec)
    }
}
//? }

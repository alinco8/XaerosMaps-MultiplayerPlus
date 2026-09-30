package dev.alinco8.xmmp.common

import dev.alinco8.xmmp.network.XMMPPacket
import net.minecraft.server.level.ServerPlayer

interface CommonEvents {
    typealias ServerPacketHandler<T> = (packet: T, player: ServerPlayer) -> Unit
    typealias ClientPacketHandler<T> = (packet: T) -> Unit

    interface PacketRegistry {
        fun <T : XMMPPacket<T>> registerToServer(
            packetType: XMMPPacket.Type<T>,
            handler: ServerPacketHandler<T>,
        )

        fun <T : XMMPPacket<T>> registerToClient(
            packetType: XMMPPacket.Type<T>,
            handler: ClientPacketHandler<T>,
        )

        fun <T : XMMPPacket<T>> registerBidirectional(
            packetType: XMMPPacket.Type<T>,
            serverHandler: ServerPacketHandler<T>,
            clientHandler: ClientPacketHandler<T>,
        )
    }

    fun registerPackets(version: String, callback: (PacketRegistry) -> Unit)
}

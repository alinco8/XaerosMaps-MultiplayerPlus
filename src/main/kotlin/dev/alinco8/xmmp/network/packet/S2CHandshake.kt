package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.config.ServerConfig
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec

data class S2CHandshake(
    val serverConfig: ServerConfig,
) : XMMPPacket<S2CHandshake>(Companion) {
    companion object : Type<S2CHandshake>(S2CHandshake::class.java) {
        override fun id() = packetId("s2c_handshake")

        override val codec = XMMPStreamCodec.composite(
            ServerConfig.codecForClient, S2CHandshake::serverConfig,
            ::S2CHandshake,
        )
    }
}

package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.config.ServerConfig
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class S2CHandshake(
    val sharedConfig: ServerConfig.SharedConfig,
) : XMMPPacket<S2CHandshake>(Companion) {
    companion object : Type<S2CHandshake>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                ServerConfig.SharedConfig.codec, S2CHandshake::sharedConfig,
                ::S2CHandshake,
            )
        }
    }
}

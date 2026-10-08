package dev.alinco8.xmmp.client.network.standalone

import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.client.network.ClientPacketHandlers
import dev.alinco8.xmmp.core.LOGGER
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class StandaloneInboundHandler(
    private val sender: StandalonePacketSender,
) : SimpleChannelInboundHandler<ByteBuf>() {
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        LOGGER.error("Exception caught in StandaloneInboundHandler", cause)
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        Minecraft.getInstance().execute {
            if (!sender.isClosed) XMMPClient.onStandaloneDisconnected(sender)
        }

        ctx.fireChannelInactive()
    }

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        buf: ByteBuf,
    ) {
        ClientPacketHandlers.handle(buf) {
            XMMPClient.packetSender === sender && !sender.isClosed
        }
    }
}

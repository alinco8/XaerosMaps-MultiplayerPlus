package dev.alinco8.xmmp.standalone

import dev.alinco8.xmmp.core.LOGGER
import dev.alinco8.xmmp.core.network.readByteArray
import dev.alinco8.xmmp.core.server.ServerSession
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.UUID
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

class StandaloneInboundHandler(
    private val session: ServerSession,
    private val host: StandaloneServerHost,
    private val executor: ScheduledThreadPoolExecutor,
    private val stopping: AtomicBoolean,
) : SimpleChannelInboundHandler<ByteBuf>() {
    private var playerId = UUID.randomUUID()

    override fun channelActive(ctx: ChannelHandlerContext) {
        if (!stopping.get()) executor.execute {
            host.register(playerId, ctx.channel())
            session.onPlayerJoin(playerId)
        }

        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (!stopping.get()) executor.execute {
            host.unregister(playerId)
            session.onPlayerLeave(playerId)
        }

        ctx.fireChannelInactive()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        LOGGER.error("Exception caught in StandaloneInboundHandler", cause)
        ctx.close()
    }

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        buf: ByteBuf,
    ) {
        if (stopping.get()) return

        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)

        executor.execute { session.handlers.handle(bytes, playerId) }
    }
}

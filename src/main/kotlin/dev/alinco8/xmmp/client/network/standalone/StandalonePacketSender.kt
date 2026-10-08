package dev.alinco8.xmmp.client.network.standalone

import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.client.network.ClientPacketSender
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.writeId
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.util.concurrent.atomic.AtomicBoolean
import net.minecraft.client.Minecraft

class StandalonePacketSender(
    private val host: String,
    private val port: Int,
) : ClientPacketSender {
    private val group = NioEventLoopGroup()
    private val closed = AtomicBoolean(false)

    @Volatile
    private var channel: Channel? = null

    val isClosed: Boolean
        get() = closed.get()

    fun connect(onResult: (Throwable?) -> Unit) {
        try {
            val future = Bootstrap()
                .group(group)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(
                            VarIntFrameDecoder(),
                            VarIntFrameEncoder(),
                            StandaloneInboundHandler(this@StandalonePacketSender)
                        )
                    }
                })
                .connect(host, port)

            channel = future.channel()

            if (closed.get()) channel?.close()

            future.addListener(ChannelFutureListener { result ->
                if (closed.get()) {
                    result.channel().close()
                } else if (result.isSuccess) {
                    onResult(null)
                } else {
                    close()
                    onResult(result.cause())
                }
            })
        } catch (e: Exception) {
            close()
            onResult(e)
        }
    }

    override fun <T : XMMPPacket<T>> send(packet: T) {
        val ch = channel
        if (closed.get() || ch == null || !ch.isActive) {
            Minecraft.getInstance().execute {
                if (!isClosed) XMMPClient.onStandaloneDisconnected(this)
            }
            return
        }

        val frame = ch.alloc().buffer()

        try {
            frame.writeId(packet.type.id)
            packet.type.codec.encode(frame, packet)
        } catch (e: Exception) {
            frame.release()
            throw e
        }

        ch.writeAndFlush(frame)
            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        channel?.close()
        group.shutdownGracefully()
    }
}

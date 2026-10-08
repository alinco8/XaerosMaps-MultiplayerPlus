package dev.alinco8.xmmp.standalone

import dev.alinco8.xmmp.core.LOGGER
import dev.alinco8.xmmp.core.server.ServerSession
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.Path


fun main() {
    val root = Path("run").toAbsolutePath()
    val port = System.getenv("STANDALONE_PORT")?.toIntOrNull() ?: 25580

    val stopping = AtomicBoolean(false)
    val shutdownLock = Any()

    val group = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    val executor = ScheduledThreadPoolExecutor(1)

    LOGGER.info("Starting standalone server in $root")

    val host = StandaloneServerHost()
    val session = try {
        ServerSession(
            root.resolve("config.toml").toFile(),
            root.resolve("data"),
            host,
            standalone = true
        )
    } catch (e: Exception) {
        executor.shutdown()
        group.shutdownGracefully().syncUninterruptibly()
        throw e
    }

    val tick = executor.scheduleAtFixedRate(
        { session.onTickPost() },
        0, 50, TimeUnit.MILLISECONDS
    )

    fun shutdown() {
        synchronized(shutdownLock) {
            if (!stopping.compareAndSet(false, true)) return
            LOGGER.info("Shutting down standalone server")

            tick.cancel(false)

            try {
                group.shutdownGracefully(
                    0, 5, TimeUnit.SECONDS
                ).syncUninterruptibly()
            } finally {
                try {
                    CompletableFuture.runAsync({ session.close() }, executor).join()
                } finally {
                    executor.shutdown()
                }
            }

            LOGGER.info("Standalone server stopped")
        }
    }

    val hook = Thread({ shutdown() }, "xmmp-shutdown")
    Runtime.getRuntime().addShutdownHook(hook)

    try {
        val channel = ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        VarIntFrameDecoder(),
                        VarIntFrameEncoder(),
                        StandaloneInboundHandler(
                            session,
                            host,
                            executor,
                            stopping
                        )
                    )
                }
            })
            .bind(port)
            .sync()
            .channel()

        LOGGER.info("Server started on port $port")
        channel.closeFuture().sync()
    } finally {
        shutdown()
    }
}

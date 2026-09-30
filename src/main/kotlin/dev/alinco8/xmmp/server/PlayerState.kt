package dev.alinco8.xmmp.server

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.config.ServerConfig
import dev.alinco8.xmmp.network.CreditWindow
import dev.alinco8.xmmp.network.TokenBucket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level

class PlayerState(
    parentScope: CoroutineScope,
    serverConfig: ServerConfig,
) : AutoCloseable {
    class PendingUpload(
        val level: ServerLevel,
        val dimension: ResourceKey<Level>,
        val chunkPos: ChunkKey,
        val seq: Long,
        val payload: ByteArray,
    ) {
        operator fun component1() = level
        operator fun component2() = chunkPos
        operator fun component3() = payload
    }

    val uploadLimiter = TokenBucket(serverConfig.uploadBurst, serverConfig.uploadRateLimit)
    val downloadLimiter = TokenBucket(serverConfig.downloadBurst, serverConfig.downloadRateLimit)
    val regionRequestLimiter =
        TokenBucket(serverConfig.regionRequestBurst, serverConfig.regionRequestRateLimit)

    val scope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext.job)
    )

    val uploads = Channel<PendingUpload>(serverConfig.uploadWindow + 16)
    val outbound = Outbound()

    @Volatile
    var downloadCredits = CreditWindow(1)

    var syncId = -1
    var handshakeSent = false
    var syncedDimension: ResourceKey<Level>? = null
    var enableWorldMapSync = false

    override fun close() {
        uploads.close()
        scope.cancel()
    }
}

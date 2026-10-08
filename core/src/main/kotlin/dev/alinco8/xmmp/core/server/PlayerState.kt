package dev.alinco8.xmmp.core.server

import dev.alinco8.xmmp.core.ChunkKey
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.config.ServerConfig
import dev.alinco8.xmmp.core.network.CreditWindow
import dev.alinco8.xmmp.core.network.TokenBucket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.job

class PlayerState(
    parentScope: CoroutineScope,
    serverConfig: ServerConfig,
) : AutoCloseable {
    class PendingUpload(
        val dimension: ResourceId,
        val layer: SyncLayer,
        val chunkPos: ChunkKey,
        val seq: Long,
        val payload: ByteArray,
    )

    val uploadLimiter =
        TokenBucket(serverConfig.shared.uploadBurst, serverConfig.shared.uploadRateLimit)
    val downloadLimiter = TokenBucket(serverConfig.downloadBurst, serverConfig.downloadRateLimit)
    val regionRequestLimiter =
        TokenBucket(
            serverConfig.shared.regionRequestBurst,
            serverConfig.shared.regionRequestRateLimit
        )

    val scope = CoroutineScope(
        parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext.job)
    )

    val uploads = Channel<PendingUpload>(serverConfig.shared.uploadWindow + 16)
    val outbound = Outbound()

    @Volatile
    var downloadCredits = CreditWindow(1)

    var syncId = -1
    var handshakeSent = false
    var syncedDimension: ResourceId? = null
    var enableWorldMapSync = false

    override fun close() {
        uploads.close()
        scope.cancel()
    }
}

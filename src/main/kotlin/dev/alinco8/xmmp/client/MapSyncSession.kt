package dev.alinco8.xmmp.client

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.client.io.CursorStore
import dev.alinco8.xmmp.client.sync.ChunkDownloader
import dev.alinco8.xmmp.client.sync.ChunkUploader
import dev.alinco8.xmmp.client.sync.ExclusiveJob
import dev.alinco8.xmmp.client.sync.UploadFlow
import dev.alinco8.xmmp.client.xaero.TileSnapshot
import dev.alinco8.xmmp.client.xaero.XaeroController
import dev.alinco8.xmmp.client.xaero.XaeroController.writeTile
import dev.alinco8.xmmp.client.xaero.awaitResult
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.utils.ModPaths
import dev.alinco8.xmmp.io.TilePayloadCodec
import dev.alinco8.xmmp.io.PayloadHash
import dev.alinco8.xmmp.network.TokenBucket
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.minecraft.network.FriendlyByteBuf
import xaero.map.core.XaeroWorldMapCore

class MapSyncSession(
    val key: MapKey,
    regionRequestLimiter: TokenBucket,
    private val uploadFlow: UploadFlow,
    scope: CoroutineScope,
) {
    val dimension get() = key.dimension

    private val job = SupervisorJob(scope.coroutineContext.job)
    private val syncScope = scope + job

    private val uploader = ChunkUploader(dimension, uploadFlow)
    private val downloader = ChunkDownloader(dimension, regionRequestLimiter)

    private val cursorStore = CompletableDeferred<CursorStore>()

    private val writingJobs = ConcurrentHashMap<RegionKey, MutableSet<Job>>()
    private val failedRegions = ConcurrentHashMap.newKeySet<RegionKey>()
    private val appliedRevisions = ConcurrentHashMap<ChunkKey, Long>()
    private val latestRevisions = ConcurrentHashMap<ChunkKey, Long>()

    private val uploaderTickJob = ExclusiveJob {
        syncScope.launch { uploader.tick() }
    }

    var syncId = 0

    init {
        val processor = XaeroWorldMapCore.currentSession?.mapProcessor
            ?: error("Map processor not found, is Xaero's World Map mod loaded?")

        var base = ModPaths.gameDir()
            .resolve("xmmp")
            .resolve(key.mainId)
            .resolve(processor.getDimensionName(key.dimension))
        if (key.multiworldId != null) {
            base = base.resolve(key.multiworldId)
        }

        syncScope.launch { cursorStore.complete(CursorStore.open(base)) }

        syncScope.launch {
            while (isActive) {
                cursorStore.await().flush()

                delay(XMMPConfig.HANDLER.instance().flushInterval.milliseconds)
            }
        }
    }

    fun onTickPost() {
        uploaderTickJob.launch()
    }

    fun onTileWritten(x: Int, z: Int, snap: TileSnapshot) {
        syncScope.launch(Dispatchers.Default) {
            val buf = FriendlyByteBuf(Unpooled.buffer())
            val payload = try {
                TilePayloadCodec.encode(buf, snap.tileData, snap.blocks)
                ByteBufUtil.getBytes(buf)
            } finally {
                buf.release()
            }

            uploader.offer(ChunkKey(x, z), payload, PayloadHash.of(payload))
        }
    }

    fun onRegionIndex(regionRevisions: Map<RegionKey, Long>) {
        if (key.usingWorldSave) return

        syncScope.launch {
            downloader.onRegionIndex(cursorStore.await(), regionRevisions) { region ->
                failedRegions.remove(region)
            }
        }
    }

    fun onChunkData(chunkPos: ChunkKey, revision: Long, payload: ByteArray): Job? {
        val applied = appliedRevisions[chunkPos]
        if (applied != null && applied >= revision) return null

        var accepted = false
        latestRevisions.compute(chunkPos) { _, cur ->
            if (cur == null || revision > cur) {
                accepted = true
                revision
            } else cur
        }
        if (!accepted) return null

        val regionPos = chunkPos.toRegion()

        val job = syncScope.launch {
            try {
                val (tileData, blocks) = withContext(Dispatchers.Default) {
                    TilePayloadCodec.decode(
                        FriendlyByteBuf(Unpooled.wrappedBuffer(payload))
                    )
                } ?: run {
                    LOGGER.warn(
                        "Failed to decode tile for chunk {}:{}",
                        chunkPos.globalX,
                        chunkPos.globalZ
                    )
                    failedRegions.add(regionPos)

                    return@launch
                }

                val written = awaitResult(maxAttempts = 20 * 60) {
                    if (latestRevisions[chunkPos] != revision) return@awaitResult XaeroController.Result.Success(
                        false
                    )

                    val processor = XaeroWorldMapCore.currentSession?.mapProcessor
                        ?: return@awaitResult XaeroController.Result.NotFound

                    processor.writeTile(
                        dimension,
                        chunkPos,
                        tileData,
                        blocks
                    ).map { true }
                } ?: run {
                    LOGGER.warn(
                        "Failed to write tile for chunk {}:{}",
                        chunkPos.globalX,
                        chunkPos.globalZ
                    )
                    failedRegions.add(regionPos)

                    return@launch
                }

                when (written) {
                    true -> appliedRevisions.merge(chunkPos, revision, ::maxOf)
                    false -> {}
                }
            } finally {
                latestRevisions.remove(chunkPos, revision)
            }
        }

        writingJobs.computeIfAbsent(regionPos) { ConcurrentHashMap.newKeySet() }.add(job)
        job.invokeOnCompletion { writingJobs[regionPos]?.remove(job) }

        return job
    }

    fun onRegionSyncDone(regionPos: RegionKey, revision: Long, syncId: Int) {
        if (syncId != this.syncId) return

        val jobs = writingJobs[regionPos]?.toList().orEmpty()

        syncScope.launch {
            jobs.joinAll()
            if (failedRegions.remove(regionPos)) return@launch

            cursorStore.await().setCursor(regionPos, revision)
        }
    }

    fun close() {
        job.cancel()

        runBlocking {
            withTimeoutOrNull(1.seconds) {
                @OptIn(ExperimentalCoroutinesApi::class)
                if (cursorStore.isCompleted) cursorStore.getCompleted().flush()
            }
        }
    }
}

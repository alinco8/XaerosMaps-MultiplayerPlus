package dev.alinco8.xmmp.client

import dev.alinco8.xmmp.core.ChunkKey
import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.client.io.CursorStore
import dev.alinco8.xmmp.client.sync.ChunkDownloader
import dev.alinco8.xmmp.client.sync.ChunkUploader
import dev.alinco8.xmmp.client.sync.ExclusiveJob
import dev.alinco8.xmmp.client.sync.UploadFlow
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.client.xaero.TileSnapshot
import dev.alinco8.xmmp.client.xaero.XaeroController
import dev.alinco8.xmmp.client.xaero.XaeroController.writeTile
import dev.alinco8.xmmp.client.xaero.awaitResult
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.core.config.ServerConfig
import dev.alinco8.xmmp.utils.ModPaths
import dev.alinco8.xmmp.io.TilePayloadCodec
import dev.alinco8.xmmp.core.io.PayloadHash
import dev.alinco8.xmmp.core.network.TokenBucket
import dev.alinco8.xmmp.id
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
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import xaero.map.core.XaeroWorldMapCore

class MapSyncContext(
    val key: MapKey,
    val world: WorldContext,
)

class MapSyncSession(
    private val ctx: MapSyncContext,
    regionRequestLimiter: TokenBucket,
    uploadFlow: UploadFlow,
    scope: CoroutineScope,
) {
    private class LayerData(
        val uploader: ChunkUploader,
        val cursorStore: CompletableDeferred<CursorStore>,
    ) {
        val writingJobs = ConcurrentHashMap<RegionKey, MutableSet<Job>>()
        val failedRegions = ConcurrentHashMap.newKeySet<RegionKey>()
        val appliedRevisions = ConcurrentHashMap<ChunkKey, Long>()
        val latestRevisions = ConcurrentHashMap<ChunkKey, Long>()
    }

    val dimension get() = ctx.key.dimension

    private val job = SupervisorJob(scope.coroutineContext.job)
    private val syncScope = scope + job

    private val layerData: Map<SyncLayer, LayerData> = SyncLayer.entries.associateWith { layer ->
        LayerData(
            ChunkUploader(ctx, dimension.id(), layer, uploadFlow),
            CompletableDeferred(),
        )
    }.toMap()
    private val downloader = ChunkDownloader(ctx, dimension.id(), regionRequestLimiter)

    private val uploaderTickJob = ExclusiveJob {
        syncScope.launch {
            layerData.values.forEach {
                it.uploader.tick()
            }
        }
    }

    var syncId = 0

    init {

        val processor = XaeroWorldMapCore.currentSession?.mapProcessor
            ?: error("Map processor not found, is Xaero's World Map mod loaded?")

        var base = ModPaths.gameDir()
            .resolve("xmmp")
            .resolve(ctx.key.mainId)
            .resolve(
                processor.getDimensionName(ctx.key.dimension)
            )
        if (ctx.key.multiworldId != null) {
            base = base.resolve(ctx.key.multiworldId)
        }

        layerData.forEach { (layer, data) ->
            syncScope.launch {
                data.cursorStore.complete(
                    CursorStore.open(layer.dirName(base))
                )
            }
        }

        syncScope.launch {
            while (isActive) {
                layerData.values.forEach { it.cursorStore.await().flush() }

                delay(XMMPConfig.HANDLER.instance().flushInterval.milliseconds)
            }
        }
    }

    fun onTickPost() {
        uploaderTickJob.launch()
    }

    fun onTileWritten(layer: Int, x: Int, z: Int, snap: TileSnapshot) {
        val layer = SyncLayer.of(layer) ?: return
        if (!ctx.world.sharedConfig.syncCaves && layer == SyncLayer.FULL_CAVE) return

        syncScope.launch(Dispatchers.Default) {
            val buf = FriendlyByteBuf(Unpooled.buffer())
            val payload = try {
                TilePayloadCodec.encode(buf, snap.tileData, snap.blocks)
                ByteBufUtil.getBytes(buf)
            } finally {
                buf.release()
            }

            layerData[layer]!!.uploader.offer(
                ChunkKey(x, z),
                payload,
                PayloadHash.of(payload)
            )
        }
    }

    fun onRegionIndex(layer: SyncLayer, regionRevisions: Map<RegionKey, Long>) {
        if (ctx.key.usingWorldSave) return

        syncScope.launch {
            val layerData = layerData[layer]!!

            downloader.onRegionIndex(
                layer,
                layerData.cursorStore.await(),
                regionRevisions
            ) { region ->
                layerData.failedRegions.remove(region)
            }
        }
    }

    fun onChunkData(
        chunkPos: ChunkKey,
        layer: SyncLayer,
        revision: Long,
        payload: ByteArray,
    ): Job? {
        val layerData = layerData[layer]!!

        val applied = layerData.appliedRevisions[chunkPos]
        if (applied != null && applied >= revision) return null

        var accepted = false
        layerData.latestRevisions.compute(chunkPos) { _, cur ->
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
                    layerData.failedRegions.add(regionPos)

                    return@launch
                }

                val written = awaitResult(maxAttempts = 20 * 60) {
                    if (layerData.latestRevisions[chunkPos] != revision) return@awaitResult XaeroController.Result.Success(
                        false
                    )

                    val processor = XaeroWorldMapCore.currentSession?.mapProcessor
                        ?: return@awaitResult XaeroController.Result.NotFound

                    processor.writeTile(
                        dimension,
                        chunkPos,
                        layer,
                        tileData,
                        blocks
                    ).map { true }
                } ?: run {
                    LOGGER.warn(
                        "Failed to write tile for chunk {}:{}",
                        chunkPos.globalX,
                        chunkPos.globalZ
                    )
                    layerData.failedRegions.add(regionPos)

                    return@launch
                }

                when (written) {
                    true -> layerData.appliedRevisions.merge(chunkPos, revision, ::maxOf)
                    false -> {}
                }
            } finally {
                layerData.latestRevisions.remove(chunkPos, revision)
            }
        }

        layerData.writingJobs.computeIfAbsent(regionPos) { ConcurrentHashMap.newKeySet() }.add(job)
        job.invokeOnCompletion { layerData.writingJobs[regionPos]?.remove(job) }

        return job
    }

    fun onRegionSyncDone(layer: SyncLayer, regionPos: RegionKey, revision: Long, syncId: Int) {
        if (syncId != this.syncId) return

        val layerData = layerData[layer]!!
        val jobs = layerData.writingJobs[regionPos]?.toList().orEmpty()

        syncScope.launch {
            jobs.joinAll()
            if (layerData.failedRegions.remove(regionPos)) return@launch

            layerData.cursorStore.await().setCursor(regionPos, revision)
        }
    }

    fun close() {
        job.cancel()

        runBlocking {
            withTimeoutOrNull(1.seconds) {
                layerData.values.forEach {
                    @OptIn(ExperimentalCoroutinesApi::class)
                    if (it.cursorStore.isCompleted) it.cursorStore.getCompleted().flush()
                }
            }
        }
    }
}

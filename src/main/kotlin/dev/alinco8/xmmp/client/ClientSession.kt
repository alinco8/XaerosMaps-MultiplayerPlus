package dev.alinco8.xmmp.client

import dev.alinco8.xmmp.core.ChunkKey
import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.core.TickDispatcher
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.client.network.ClientPacketSender
import dev.alinco8.xmmp.client.sync.UploadFlow
import dev.alinco8.xmmp.client.update.UpdateChecker
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.client.xaero.TileSnapshot
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.config.ServerConfig
import dev.alinco8.xmmp.core.network.TokenBucket
import dev.alinco8.xmmp.core.network.packet.C2SDimensionSync
import dev.alinco8.xmmp.core.network.packet.C2SDownloadAck
import dev.alinco8.xmmp.core.network.packet.C2SHandshake
import dev.alinco8.xmmp.id
import dev.alinco8.xmmp.utils.ModList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import xaero.map.core.XaeroWorldMapCore

data class MapKey(
    val mainId: String,
    val dimension: ResourceKey<Level>,
    val multiworldId: String?,
    val usingWorldSave: Boolean,
)

class WorldContext(
    val sharedConfig: ServerConfig.SharedConfig,
    val packetSender: ClientPacketSender,
)

class ClientSession(val ctx: WorldContext) {
    val isWorldMapLoaded = ModList.isModLoaded("xaeroworldmap")

    private val dispatcher = TickDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var syncSession: MapSyncSession? = null
    private val downloadCompleted = AtomicInteger()

    private val regionRequestLimiter = TokenBucket(
        ctx.sharedConfig.regionRequestBurst,
        ctx.sharedConfig.regionRequestRateLimit * 0.9,
    )
    private val uploadFlow = UploadFlow(ctx.sharedConfig)

    private var lastMapKey: MapKey? = null
    private var lastLevel: ClientLevel? = null
    private var lastSyncId = 0

    init {
        if (XMMPConfig.HANDLER.instance().checkUpdate)
            scope.launch {
                while (isActive) {
                    runCatching {
                        withTimeoutOrNull(10.seconds) {
                            UpdateChecker.checkForUpdates()
                        }
                    }

                    delay(1.minutes)
                }
            }
    }

    fun onTickPost() {
        if (isWorldMapLoaded) Minecraft.getInstance().level?.let { level ->
            val key = currentMapKey(level.dimension().id()) ?: return@let

            val keyChanged = key != lastMapKey
            if (!keyChanged && level === lastLevel) return@let
            lastMapKey = key
            lastLevel = level

            if (keyChanged) onMapKeyChanged(key)
            else sendDimensionSync(syncSession ?: return@let)
        }

        dispatcher.pump()
        syncSession?.onTickPost()

        val completed = downloadCompleted.getAndSet(0)
        if (completed > 0) ctx.packetSender.send(C2SDownloadAck(completed))

    }

    private fun currentMapKey(dimension: ResourceId): MapKey? {
        val processor = XaeroWorldMapCore.currentSession?.mapProcessor ?: return null
        if (!processor.isMapWorldUsable || processor.isWaitingForWorldUpdate || !processor.isCurrentMultiworldWritable)
            return null

        val mapDim = processor.mapWorld.currentDimensionId ?: return null
        if (mapDim.id() != dimension) return null

        val mainId = processor.currentWorldId ?: return null
        if (processor.currentDimId != processor.getDimensionName(mapDim)) return null

        return MapKey(
            mainId,
            mapDim,
            processor.currentMWId,
            processor.mapWorld.currentDimension.isUsingWorldSave
        )
    }

    fun onTileWritten(
        dimension: ResourceId,
        layer: Int,
        x: Int,
        z: Int,
        snap: TileSnapshot,
    ) =
        syncSession?.let {
            if (it.dimension.id() != dimension) {
                LOGGER.debug(
                    "Received tile written for dimension {}, but current dimension is {}. Ignoring.",
                    dimension,
                    it.dimension
                )
                return@let
            }
            it.onTileWritten(layer, x, z, snap)
        }

    fun onRegionIndex(
        dimension: ResourceId,
        layer: SyncLayer,
        regionRevisions: Map<RegionKey, Long>,
    ) {
        if (dimension != syncSession?.dimension?.id()) {
            LOGGER.warn(
                "Received region revisions for dimension {}, but current dimension is {}. Ignoring.",
                dimension,
                syncSession?.dimension
            )
            return
        }

        syncSession?.onRegionIndex(layer, regionRevisions)
    }

    fun onChunkData(
        dimension: ResourceId,
        layer: SyncLayer,
        chunkPos: ChunkKey,
        revision: Long,
        payload: ByteArray,
    ) {
        val s = syncSession
        if (s == null || dimension != s.dimension.id()) {
            downloadCompleted.incrementAndGet()
            return
        }

        val job = s.onChunkData(chunkPos, layer, revision, payload)
        if (job == null) {
            downloadCompleted.incrementAndGet()
        } else {
            job.invokeOnCompletion {
                downloadCompleted.incrementAndGet()
            }
        }
    }

    fun onMapKeyChanged(key: MapKey) {
        val s = syncSession
        syncSession = null
        s?.close()

        syncSession = MapSyncSession(
            MapSyncContext(
                key,
                ctx,
            ),
            regionRequestLimiter,
            uploadFlow,
            scope,
        )

        sendDimensionSync(syncSession ?: return)
    }

    fun onUploadAck(lastSeq: Long) = uploadFlow.onAck(lastSeq)

    fun onHandshake() {
        ctx.packetSender.send(
            C2SHandshake(
                isWorldMapLoaded,
                XMMPConfig.HANDLER.instance().downloadWindow
            )
        )
    }

    fun close() {
        scope.cancel()

        dispatcher.pump()
        dispatcher.close()

        syncSession?.close()
    }

    fun onRegionSyncDone(
        dimension: ResourceId,
        layer: SyncLayer,
        regionPos: RegionKey,
        revision: Long,
        syncId: Int,
    ) {
        if (dimension != syncSession?.dimension?.id()) return

        syncSession?.onRegionSyncDone(layer, regionPos, revision, syncId)
    }

    private fun sendDimensionSync(session: MapSyncSession) {
        val id = ++lastSyncId
        session.syncId = id
        ctx.packetSender.send(C2SDimensionSync(session.dimension.id(), id))
    }
}

package dev.alinco8.xmmp.client

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.TickDispatcher
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.client.network.ClientPacketSender
import dev.alinco8.xmmp.client.sync.UploadFlow
import dev.alinco8.xmmp.client.update.UpdateChecker
import dev.alinco8.xmmp.client.xaero.TileSnapshot
import dev.alinco8.xmmp.config.ServerConfig
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.network.TokenBucket
import dev.alinco8.xmmp.network.packet.C2SDimensionSync
import dev.alinco8.xmmp.network.packet.C2SDownloadAck
import dev.alinco8.xmmp.network.packet.C2SHandshake
import dev.alinco8.xmmp.utils.ModList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
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

class ClientSession(serverConfig: ServerConfig) {
    val isWorldMapLoaded = ModList.isModLoaded("xaeroworldmap")

    private val dispatcher = TickDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private var syncSession: MapSyncSession? = null
    private val downloadCompleted = AtomicInteger()

    private val regionRequestLimiter = TokenBucket(
        serverConfig.regionRequestBurst,
        serverConfig.regionRequestRateLimit * 0.9,
    )
    private val uploadFlow = UploadFlow(serverConfig)

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
            val key = currentMapKey(level.dimension()) ?: return@let

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
        if (completed > 0) ClientPacketSender.sendToServer(C2SDownloadAck(completed))

    }

    private fun currentMapKey(dimension: ResourceKey<Level>): MapKey? {
        val processor = XaeroWorldMapCore.currentSession?.mapProcessor ?: return null
        if (!processor.isMapWorldUsable || processor.isWaitingForWorldUpdate || !processor.isCurrentMultiworldWritable)
            return null

        val mapDim = processor.mapWorld.currentDimensionId ?: return null
        if (mapDim != dimension) return null

        val mainId = processor.currentWorldId ?: return null
        if (processor.currentDimId != processor.getDimensionName(mapDim)) return null

        return MapKey(
            mainId,
            mapDim,
            processor.currentMWId,
            processor.mapWorld.currentDimension.isUsingWorldSave
        )
    }

    fun onTileWritten(dimension: ResourceKey<Level>, x: Int, z: Int, snap: TileSnapshot) =
        syncSession?.let {
            if (it.dimension != dimension) {
                LOGGER.debug(
                    "Received tile written for dimension {}, but current dimension is {}. Ignoring.",
                    dimension,
                    it.dimension
                )
                return@let
            }

            it.onTileWritten(x, z, snap)
        }

    fun onRegionIndex(dimension: ResourceKey<Level>, regionRevisions: Map<RegionKey, Long>) {
        if (dimension != syncSession?.dimension) {
            LOGGER.warn(
                "Received region revisions for dimension {}, but current dimension is {}. Ignoring.",
                dimension,
                syncSession?.dimension
            )
            return
        }

        syncSession?.onRegionIndex(regionRevisions)
    }

    fun onChunkData(
        dimension: ResourceKey<Level>,
        chunkPos: ChunkKey,
        revision: Long,
        payload: ByteArray,
    ) {
        val s = syncSession
        if (s == null || dimension != s.dimension) {
            downloadCompleted.incrementAndGet()
            return
        }

        val job = s.onChunkData(chunkPos, revision, payload)
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
            key,
            regionRequestLimiter,
            uploadFlow,
            scope,
        )

        sendDimensionSync(syncSession ?: return)
    }

    fun onUploadAck(lastSeq: Long) = uploadFlow.onAck(lastSeq)

    fun onHandshake() {
        ClientPacketSender.sendToServer(
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
        dimension: ResourceKey<Level>,
        regionPos: RegionKey,
        revision: Long,
        syncId: Int,
    ) {
        if (dimension != syncSession?.dimension) return

        syncSession?.onRegionSyncDone(regionPos, revision, syncId)
    }

    private fun sendDimensionSync(session: MapSyncSession) {
        val id = ++lastSyncId
        session.syncId = id
        ClientPacketSender.sendToServer(C2SDimensionSync(session.dimension, id))
    }
}

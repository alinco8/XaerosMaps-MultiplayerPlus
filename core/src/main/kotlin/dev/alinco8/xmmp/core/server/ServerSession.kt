package dev.alinco8.xmmp.core.server

import dev.alinco8.xmmp.core.ChunkKey
import dev.alinco8.xmmp.core.LOGGER
import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.TickDispatcher
import dev.alinco8.xmmp.core.common.ServerHost
import dev.alinco8.xmmp.core.config.ServerConfig
import dev.alinco8.xmmp.core.io.FileChannelPool
import dev.alinco8.xmmp.core.network.CreditWindow
import dev.alinco8.xmmp.core.network.packet.C2SChunkUpload
import dev.alinco8.xmmp.core.network.packet.C2SHandshake
import dev.alinco8.xmmp.core.network.packet.S2CChunkData
import dev.alinco8.xmmp.core.network.packet.S2CHandshake
import dev.alinco8.xmmp.core.network.packet.S2CRegionIndex
import dev.alinco8.xmmp.core.network.packet.S2CRegionSyncDone
import dev.alinco8.xmmp.core.network.packet.S2CUploadAck
import dev.alinco8.xmmp.core.server.io.ServerRegionStore
import dev.alinco8.xmmp.core.server.network.ServerPacketHandlers
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.LockSupport
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class ServerSession(
    configFile: File,
    private val worldDataDir: Path,
    val host: ServerHost,
    val standalone: Boolean = false,
) : AutoCloseable {
    val handlers = ServerPacketHandlers(this)

    private val serverConfig = ServerConfig.fromToml(configFile)
    private val pool = FileChannelPool(64)

    private val dispatcher = TickDispatcher()
    private val rootJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + rootJob)

    private val playerStates = ConcurrentHashMap<UUID, PlayerState>()
    private val regionStores =
        ConcurrentHashMap<Pair<ResourceId, SyncLayer>, ServerRegionStore>()

    init {
        Files.createDirectories(worldDataDir)

        scope.launch {
            while (isActive) {
                delay(serverConfig.regionFlushInterval.milliseconds)

                runCatching {
                    regionStores.values.forEach { it.flush() }
                }.onFailure {
                    LOGGER.error("Failed to flush region stores", it)
                }
            }
        }
    }

    private fun getPlayerState(playerId: UUID) =
        playerStates[playerId] ?: error("Player state not found for player ${playerId}")

    private fun getRegionStore(dimension: ResourceId, layer: SyncLayer): ServerRegionStore {
        val dimensionDir = worldDataDir
            .resolve("world_map")
            .resolve(dimension.namespace)
            .resolve(dimension.path)

        return regionStores.computeIfAbsent(dimension to layer) {
            ServerRegionStore(pool, layer.dirName(dimensionDir))
        }
    }

    fun onTickPost() {
        dispatcher.pump()
    }

    fun onPlayerJoin(playerId: UUID) {
        val state = PlayerState(scope, serverConfig)
        playerStates[playerId] = state

        state.scope.launch { runUploadWorker(playerId, state) }
        state.scope.launch { runDownloadWorker(playerId, state) }

        state.scope.launch {
            val sent = host.sendToPlayer(
                playerId,
                S2CHandshake(serverConfig.shared)
            )

            if (sent) state.handshakeSent = true
        }
    }

    fun onPlayerChangedDimension(
        playerId: UUID,
    ) {
        val state = playerStates[playerId] ?: return

        state.syncedDimension = null
        state.outbound.clear()
    }

    fun onPlayerLeave(playerId: UUID) {
        playerStates.remove(playerId)?.close()
    }

    fun onPlayerChannelsReady(playerId: UUID) {
        val state = playerStates[playerId] ?: return
        if (state.handshakeSent) return

        val sent = host.sendToPlayer(
            playerId,
            S2CHandshake(serverConfig.shared)
        )
        if (sent) state.handshakeSent = true
    }

    fun onHandshake(playerId: UUID, packet: C2SHandshake) {
        val state = playerStates[playerId] ?: return

        state.enableWorldMapSync = packet.enableWorldMapSync
        state.downloadCredits = CreditWindow(
            minOf(packet.downloadWindow, serverConfig.maxDownloadWindow)
        )
        state.outbound.signal()
    }

    fun onDimensionSync(playerId: UUID, syncId: Int) {
        val playerState = getPlayerState(playerId)
        val dimension = host.getDimension(playerId) ?: return

        playerState.outbound.clear()
        playerState.syncedDimension = dimension
        playerState.syncId = syncId
        playerState.outbound.signal()

        playerState.scope.launch {
            val syncCaves = serverConfig.shared.syncCaves
            SyncLayer.entries.forEach { layer ->
                if (!syncCaves && layer == SyncLayer.FULL_CAVE) return@forEach

                host.sendToPlayer(
                    playerId, S2CRegionIndex(
                        dimension,
                        layer,
                        getRegionStore(dimension, layer).listRegionRevisions()
                    )
                )
            }
        }
    }

    fun onChunkUpload(playerId: UUID, packet: C2SChunkUpload) {
        val dimension = host.getDimension(playerId)
        if (dimension != packet.dimension) {
            LOGGER.warn(
                "Player {} sent chunk upload for dimension {} but is in {}, ignoring",
                playerId,
                packet.dimension,
                dimension
            )
            return
        }

        val state = playerStates[playerId] ?: return
        val upload = PlayerState.PendingUpload(
            packet.dimension,
            packet.layer,
            packet.chunkPos,
            packet.seq,
            packet.payload
        )
        if (state.uploads.trySend(upload).isFailure) {
            LOGGER.warn(
                "Player {} exceeded the upload window, disconnecting",
                playerId
            )
            host.disconnectPlayer(playerId, "[XMMP] Upload window exceeded")
        }
    }

    private suspend fun runUploadWorker(uploaderId: UUID, state: PlayerState) {
        val batch = ArrayList<PlayerState.PendingUpload>()
        val maxBatch = serverConfig.shared.uploadBurst.toInt().coerceAtLeast(1)

        for (first in state.uploads) {
            batch.add(first)
            while (batch.size < maxBatch) batch += state.uploads.tryReceive().getOrNull() ?: break

            state.uploadLimiter.waitForTokens(batch.size.toDouble())

            val syncCaves = serverConfig.shared.syncCaves
            val targets = batch.filter {
                (syncCaves || it.layer != SyncLayer.FULL_CAVE)
            }
            val validated = if (serverConfig.enableChunkValidation)
                targets.filter { host.validateChunkBytes(it.payload) } else targets

            for (upload in validated) {
                try {
                    val dimension = upload.dimension

                    getRegionStore(dimension, upload.layer).writeChunk(
                        upload.chunkPos,
                        upload.payload
                    )
                        ?: continue

                    for (playerId in host.listPlayers()) {
                        if (playerId == uploaderId) continue

                        val s = playerStates[playerId] ?: continue
                        if (!s.enableWorldMapSync || s.syncedDimension != dimension) continue

                        s.outbound.enqueueChunk(upload.layer, upload.chunkPos)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LOGGER.error(
                        "Failed to process upload from player {} for chunk at ({}, {})",
                        uploaderId,
                        upload.chunkPos.globalX,
                        upload.chunkPos.globalZ,
                        e
                    )
                }
            }

            host.sendToPlayer(uploaderId, S2CUploadAck(batch.last().seq))
            batch.clear()
        }
    }

    private suspend fun runDownloadWorker(playerId: UUID, state: PlayerState) {
        while (true) {
            state.outbound.awaitSignal()

            while (true) {
                val dimension = state.syncedDimension ?: break
                val credits = state.downloadCredits
                if (!credits.tryAcquire()) break

                val item = state.outbound.poll() ?: run {
                    credits.release(1)
                    break
                }

                when (item) {
                    is Outbound.Chunk -> {
                        val data =
                            getRegionStore(dimension, item.layer).readChunkWithRevision(item.pos)
                                ?: run {
                                    credits.release(1)
                                    continue
                                }

                        state.downloadLimiter.waitForTokens(1.0)
                        host.sendToPlayer(
                            playerId,
                            S2CChunkData(
                                dimension,
                                item.layer,
                                item.pos,
                                data.second,
                                data.first
                            )
                        )
                    }

                    is Outbound.Done -> {
                        credits.release(1)
                        host.sendToPlayer(
                            playerId,
                            S2CRegionSyncDone(
                                dimension,
                                item.layer,
                                item.region,
                                item.revision,
                                item.syncId
                            )
                        )
                    }
                }
            }
        }
    }

    fun onRegionSync(playerId: UUID, layer: SyncLayer, regionPos: RegionKey, cursor: Long) {
        if (!serverConfig.shared.syncCaves && layer == SyncLayer.FULL_CAVE) return

        val playerState = getPlayerState(playerId)
        if (!playerState.regionRequestLimiter.tryConsume(1.0)) {
            LOGGER.warn(
                "Player {} sent region sync for ({}, {}) but is rate limited, ignoring",
                playerId,
                regionPos.x,
                regionPos.z
            )
            return
        }

        val dimension = host.getDimension(playerId) ?: return
        val syncId = playerState.syncId
        val store = getRegionStore(dimension, layer)
        playerState.scope.launch {
            val file = store.regionReadonly(regionPos) ?: return@launch
            val snapshot = file.regionRevision() ?: return@launch

            for (chunkX in 0 until 32) {
                for (chunkZ in 0 until 32) {
                    val revision = file.chunkRevision(chunkX, chunkZ) ?: continue
                    if (revision <= cursor) continue
                    if (playerState.syncId != syncId) return@launch

                    playerState.outbound.enqueueChunk(
                        layer,
                        ChunkKey.fromLocal(regionPos, chunkX, chunkZ)
                    )
                }
            }

            if (playerState.syncId == syncId) playerState.outbound.enqueueDone(
                layer,
                regionPos,
                snapshot,
                syncId
            )
        }
    }

    fun onDownloadAck(playerId: UUID, completed: Int) {
        if (completed <= 0) return

        val state = playerStates[playerId] ?: return
        state.downloadCredits.release(completed)
        state.outbound.signal()
    }

    override fun close() {
        playerStates.values.forEach { it.close() }
        rootJob.cancel()

        val deadline = System.nanoTime() + 5.seconds.inWholeNanoseconds
        while (!rootJob.isCompleted && System.nanoTime() < deadline) {
            dispatcher.pump()
            LockSupport.parkNanos(1_000_000)
        }
        if (!rootJob.isCompleted)
            LOGGER.warn("Server session did not shut down cleanly, some tasks may still be running")
        dispatcher.close()

        runBlocking {
            withTimeoutOrNull(30.seconds) {
                runCatching {
                    regionStores.values.forEach { it.flush() }
                }.onFailure {
                    LOGGER.error("Failed to flush region stores", it)
                }

                pool.close()
            }
        }
    }
}

package dev.alinco8.xmmp.server

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.TickDispatcher
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.config.ServerConfig
import dev.alinco8.xmmp.id
import dev.alinco8.xmmp.server.io.ServerRegionStore
import dev.alinco8.xmmp.io.FileChannelPool
import dev.alinco8.xmmp.io.TilePayloadCodec
import dev.alinco8.xmmp.network.CreditWindow
import dev.alinco8.xmmp.network.packet.C2SChunkUpload
import dev.alinco8.xmmp.network.packet.C2SHandshake
import dev.alinco8.xmmp.network.packet.S2CChunkData
import dev.alinco8.xmmp.network.packet.S2CHandshake
import dev.alinco8.xmmp.network.packet.S2CRegionIndex
import dev.alinco8.xmmp.network.packet.S2CRegionSyncDone
import dev.alinco8.xmmp.network.packet.S2CUploadAck
import dev.alinco8.xmmp.server.network.ServerPacketSender
import io.netty.buffer.Unpooled
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.LockSupport
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.LevelResource

class ServerSession(private val server: MinecraftServer) : AutoCloseable {
    private val worldDataDir = server.getWorldPath(LevelResource("xmmp"))

    private val pool = FileChannelPool(64)
    private val serverConfig = ServerConfig.createHandler()

    private val dispatcher = TickDispatcher()
    private val rootJob = SupervisorJob()
    private val scope = CoroutineScope(dispatcher + rootJob)

    private val playerStates = ConcurrentHashMap<UUID, PlayerState>()
    private val regionStores = ConcurrentHashMap<ResourceKey<Level>, ServerRegionStore>()

    init {
        serverConfig.load()

        Files.createDirectories(worldDataDir)

        scope.launch {
            while (isActive) {
                delay(serverConfig.instance().regionFlushInterval.milliseconds)

                runCatching {
                    regionStores.values.forEach { it.flush() }
                }.onFailure {
                    LOGGER.error("Failed to flush region stores", it)
                }
            }
        }
    }

    private fun getPlayerState(playerId: UUID) =
        playerStates[playerId] ?: error("Player state not found for player $playerId")

    private fun getRegionStore(dimension: ResourceKey<Level>): ServerRegionStore {
        val id = dimension.id()
        val dimensionDir = worldDataDir.resolve("world_map")
            .resolve(id.namespace).resolve(id.path)

        return regionStores.computeIfAbsent(dimension) {
            ServerRegionStore(pool, dimensionDir)
        }
    }

    fun onTickPost() {
        dispatcher.pump()
    }

    fun onPlayerJoin(player: ServerPlayer) {
        val state = PlayerState(scope, serverConfig.instance())
        playerStates[player.uuid] = state

        state.scope.launch { runUploadWorker(player.uuid, state) }
        state.scope.launch { runDownloadWorker(player.uuid, state) }

        val sent = ServerPacketSender.sendToPlayer(
            player,
            S2CHandshake(serverConfig.instance())
        )

        if (sent) state.handshakeSent = true
    }

    fun onPlayerChangedDimension(
        player: ServerPlayer,
    ) {
        val state = playerStates[player.uuid] ?: return

        state.syncedDimension = null
        state.outbound.clear()
    }

    fun onPlayerLeave(player: ServerPlayer) {
        playerStates.remove(player.uuid)?.close()
    }

    fun onHandshake(player: ServerPlayer, packet: C2SHandshake) {
        val state = playerStates[player.uuid] ?: return

        state.enableWorldMapSync = packet.enableWorldMapSync
        state.downloadCredits = CreditWindow(
            minOf(packet.downloadWindow, serverConfig.instance().maxDownloadWindow)
        )
        state.outbound.signal()
    }

    fun onDimensionSync(player: ServerPlayer, syncId: Int) {
        val playerState = getPlayerState(player.uuid)
        val dimension = player.level().dimension()

        playerState.outbound.clear()
        playerState.syncedDimension = dimension
        playerState.syncId = syncId
        playerState.outbound.signal()

        playerState.scope.launch {
            ServerPacketSender.sendToPlayer(
                player, S2CRegionIndex(
                    dimension,
                    getRegionStore(dimension).listRegionRevisions()
                )
            )
        }
    }

    fun onChunkUpload(player: ServerPlayer, packet: C2SChunkUpload) {
        val state = playerStates[player.uuid] ?: return
        val upload = PlayerState.PendingUpload(
            player.level() as ServerLevel,
            packet.dimension,
            packet.chunkPos,
            packet.seq,
            packet.payload
        )
        if (state.uploads.trySend(upload).isFailure) {
            LOGGER.warn(
                "Player {} exceeded the upload window, disconnecting",
                player.uuid
            )
            player.connection.disconnect(
                Component.literal("[XMMP] Upload window exceeded")
            )
        }
    }

    private suspend fun runUploadWorker(uploaderId: UUID, state: PlayerState) {
        val batch = ArrayList<PlayerState.PendingUpload>()
        val maxBatch = serverConfig.instance().uploadBurst.toInt().coerceAtLeast(1)

        for (first in state.uploads) {
            batch.add(first)
            while (batch.size < maxBatch) batch += state.uploads.tryReceive().getOrNull() ?: break

            state.uploadLimiter.waitForTokens(batch.size.toDouble())

            val sameDim = batch.filter { it.dimension == it.level.dimension() }
            for (upload in validate(uploaderId, sameDim)) {
                try {
                    val level = upload.level
                    val dimension = level.dimension()

                    getRegionStore(dimension).writeChunk(upload.chunkPos, upload.payload)
                        ?: continue

                    for (player in level.players()) {
                        if (player.uuid == uploaderId) continue

                        val s = playerStates[player.uuid] ?: continue
                        if (!s.enableWorldMapSync || s.syncedDimension != dimension) continue

                        s.outbound.enqueueChunk(upload.chunkPos)
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

            server.playerList.getPlayer(uploaderId)?.let {
                ServerPacketSender.sendToPlayer(it, S2CUploadAck(batch.last().seq))
            }
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

                val player = server.playerList.getPlayer(playerId) ?: run {
                    credits.release(1)
                    return
                }

                when (item) {
                    is Outbound.Chunk -> {
                        val data = getRegionStore(dimension).readChunkWithRevision(item.pos)
                            ?: run {
                                credits.release(1)
                                continue
                            }

                        state.downloadLimiter.waitForTokens(1.0)
                        ServerPacketSender.sendToPlayer(
                            player,
                            S2CChunkData(
                                dimension,
                                item.pos,
                                data.second,
                                data.first
                            )
                        )
                    }

                    is Outbound.Done -> {
                        credits.release(1)
                        ServerPacketSender.sendToPlayer(
                            player,
                            S2CRegionSyncDone(
                                dimension,
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

    private suspend fun validate(
        uploader: UUID,
        batch: List<PlayerState.PendingUpload>,
    ): List<PlayerState.PendingUpload> {
        if (!serverConfig.instance().enableChunkValidation) return batch

        return withContext(Dispatchers.Default) {
            batch.filter { upload ->
                val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(upload.payload))
                val ok = TilePayloadCodec.decode(buf) != null && !buf.isReadable

                if (!ok) LOGGER.warn(
                    "Player {} sent invalid chunk data for ({}, {}), ignoring",
                    uploader,
                    upload.chunkPos.globalX,
                    upload.chunkPos.globalZ
                )

                ok
            }
        }
    }

    fun onRegionSync(player: ServerPlayer, regionPos: RegionKey, cursor: Long) {
        val playerState = getPlayerState(player.uuid)
        if (!playerState.regionRequestLimiter.tryConsume(1.0)) {
            LOGGER.warn(
                "Player {} sent region sync for ({}, {}) but is rate limited, ignoring",
                player.uuid,
                regionPos.x,
                regionPos.z
            )
            return
        }

        val dimension = player.level().dimension()
        val syncId = playerState.syncId
        val store = getRegionStore(dimension)
        playerState.scope.launch {
            val file = store.regionReadonly(regionPos) ?: return@launch
            val snapshot = file.regionRevision() ?: return@launch

            for (chunkX in 0 until 32) {
                for (chunkZ in 0 until 32) {
                    val revision = file.chunkRevision(chunkX, chunkZ) ?: continue
                    if (revision <= cursor) continue
                    if (playerState.syncId != syncId) return@launch

                    playerState.outbound.enqueueChunk(
                        ChunkKey.fromLocal(regionPos, chunkX, chunkZ)
                    )
                }
            }

            if (playerState.syncId == syncId) playerState.outbound.enqueueDone(
                regionPos,
                snapshot,
                syncId
            )
        }
    }

    fun onDownloadAck(player: ServerPlayer, completed: Int) {
        if (completed <= 0) return

        val state = playerStates[player.uuid] ?: return
        state.downloadCredits.release(completed)
        state.outbound.signal()
    }

    fun onPlayerChannelsReady(player: ServerPlayer) {
        val state = playerStates[player.uuid] ?: return
        if (state.handshakeSent) return

        val sent = ServerPacketSender.sendToPlayer(
            player,
            S2CHandshake(serverConfig.instance())
        )
        if (sent) state.handshakeSent = true
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

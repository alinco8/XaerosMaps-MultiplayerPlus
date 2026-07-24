package dev.alinco8.xmmp

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier

*///? } else {
import net.minecraft.resources.ResourceLocation as Identifier

//? }

import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.io.XMMPRegionCache
import dev.alinco8.xmmp.packet.C2SChunkRowRequestPacket
import dev.alinco8.xmmp.packet.C2SXaeroReadyPacket
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import io.netty.buffer.Unpooled
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal fun ResourceKey<*>.id() =
        /*?if >=1.21.11 {*/ /*this.identifier() *//*?} else {*/ this.location() /*?}*/

internal fun loc(path: String): Identifier =
//? if forge || >=1.21 {
    Identifier.fromNamespaceAndPath(XMMP.MOD_ID, path)
//? } else {
/*Identifier(XMMP.MOD_ID, path)
*///? }

object XMMP {
    const val MOD_ID = "xmmp"
    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    const val REGION_SIZE = 32
    const val SECOND_IN_NANOS = 1_000_000_000L

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var regionCache: XMMPRegionCache
    private var flushJob: Job? = null

    private val initializedPlayers = ConcurrentHashMap.newKeySet<UUID>()

    private val uploadBuckets = ConcurrentHashMap<UUID, TokenBucket>()

    private val rowRequestBuckets = ConcurrentHashMap<UUID, TokenBucket>()

    private class TokenBucket(private val capacity: Double, private val refillPerNano: Double) {
        private var tokens = capacity
        private var last = System.nanoTime()

        @Synchronized
        fun tryConsume(): Boolean {
            val now = System.nanoTime()
            tokens = minOf(capacity, tokens + (now - last) * refillPerNano)
            last = now
            if (tokens >= 1.0) {
                tokens -= 1.0
                return true
            }
            return false
        }
    }

    private fun Player.dimensionId(): String =
        level().dimension().id().toString()


    fun onInitialize() {
        LOGGER.debug("Initializing XMMP")

        XMMPConfig.HANDLER.load()
    }

    fun onServerStarted(server: MinecraftServer) {
        val worldDir = server.getWorldPath(LevelResource("xmmp/world_map"))
        worldDir.createDirectories()
        regionCache = XMMPRegionCache(worldDir)

        flushJob = scope.launch {
            while (isActive) {
                delay(XMMPConfig.HANDLER.instance().flushInterval.milliseconds)
                runCatching {
                    regionCache.flush()
                }.onFailure { LOGGER.error("XMMP flush failed", it) }
            }
        }
    }

    fun onServerStopping() {
        flushJob?.cancel()

        if (::regionCache.isInitialized) {
            runCatching {
                regionCache.flush()
            }.onFailure { LOGGER.error("XMMP flush failed", it) }

            regionCache.close()
        }
    }

    fun onPlayerDimensionChange(player: Player) {
        initializedPlayers.remove(player.uuid)
    }

    fun onPlayerLeave(player: Player) {
        initializedPlayers.remove(player.uuid)
        uploadBuckets.remove(player.uuid)
        rowRequestBuckets.remove(player.uuid)
    }

    fun handleChunkDataPacket(packet: ChunkDataPacket, sender: Player) {
        if (packet.dimension != sender.dimensionId()) {
            LOGGER.warn(
                "Player {} sent chunk for dimension {} but is in {}, ignoring",
                sender.uuid,
                packet.dimension,
                sender.dimensionId()
            )
            return
        }

        val rate = XMMPConfig.HANDLER.instance().maxChunkUploadsPerSecond.toDouble()
        val bucket =
            uploadBuckets.computeIfAbsent(sender.uuid) { TokenBucket(rate, rate / SECOND_IN_NANOS) }
        if (!bucket.tryConsume()) {
            LOGGER.debug("Player {} exceeded chunk upload rate limit, dropping packet", sender.uuid)
            return
        }

        val buf = Unpooled.buffer()
        ChunkDataPacket.encode(FriendlyByteBuf(buf), packet)

        val raw = ByteArray(buf.readableBytes()).also { buf.readBytes(it) }
        buf.release()

        regionCache.writeChunk(packet.dimension, packet.chunkX, packet.chunkZ, raw)

        NetworkUtils.sendToPlayersInDimension(
            sender.level() as ServerLevel,
            packet,
            sender as ServerPlayer
        )
    }

    fun handleXaeroReadyPacket(packet: C2SXaeroReadyPacket, player: Player) {
        if (packet.dimension != player.dimensionId()) {
            LOGGER.warn(
                "Player {} sent XaeroReady for dimension {} but is in {}, ignoring",
                player.uuid,
                packet.dimension,
                player.dimensionId()
            )
            return
        }

        if (!initializedPlayers.add(player.uuid)) {
            LOGGER.debug("Player {} sent duplicate XaeroReady packet", player.uuid)

            return
        }

        val response = S2CRegionTimestampsPacket(
            regionCache.getAllRegionTimestamps(packet.dimension), packet.dimension
        )

        NetworkUtils.sendToPlayer(player as ServerPlayer, response)
    }

    fun handleChunkRowRequestPacket(packet: C2SChunkRowRequestPacket, player: Player) {
        if (packet.dimension != player.dimensionId()) {
            LOGGER.warn(
                "Player {} requested chunk row for dimension {} but is in {}, ignoring",
                player.uuid,
                packet.dimension,
                player.dimensionId()
            )
            return
        }

        if (packet.row !in 0 until REGION_SIZE) {
            LOGGER.warn(
                "Player {} requested out-of-range chunk row {}, ignoring",
                player.uuid,
                packet.row
            )
            return
        }

        val rate = XMMPConfig.HANDLER.instance().maxChunkRowRequestsPerSecond.toDouble()
        val bucket =
            rowRequestBuckets.computeIfAbsent(player.uuid) {
                TokenBucket(
                    rate,
                    rate / SECOND_IN_NANOS
                )
            }
        if (!bucket.tryConsume()) {
            LOGGER.debug(
                "Player {} exceeded chunk row request rate limit, dropping packet",
                player.uuid
            )
            return
        }

        repeat(REGION_SIZE) { x ->
            val chunkX = packet.regionX * REGION_SIZE + x
            val chunkZ = packet.regionZ * REGION_SIZE + packet.row

            val chunkPacketBytes =
                regionCache.readChunk(packet.dimension, chunkX, chunkZ) ?: return@repeat
            val buf = Unpooled.wrappedBuffer(chunkPacketBytes)

            LOGGER.debug(
                "Sending chunk: region=({}, {}), row={}, chunk=({}, {})",
                packet.regionX,
                packet.regionZ,
                packet.row,
                chunkX,
                chunkZ
            )
            NetworkUtils.sendToPlayer(
                player as ServerPlayer,
                try {
                    ChunkDataPacket.decode(FriendlyByteBuf(buf))
                } finally {
                    buf.release()
                }
            )
        }
    }
}

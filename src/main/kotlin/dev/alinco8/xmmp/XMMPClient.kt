package dev.alinco8.xmmp

import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.packet.C2SXaeroReadyPacket
import dev.alinco8.xmmp.packet.ChunkDataPacket
import dev.alinco8.xmmp.packet.S2CRegionTimestampsPacket
import dev.alinco8.xmmp.sync.ChunkPosQueue
import dev.alinco8.xmmp.sync.RegionSyncProcessor
import net.minecraft.client.Minecraft
import net.minecraft.world.level.ChunkPos
import xaero.map.core.XaeroWorldMapCore

object XMMPClient {
    val chunkSendQueue = ChunkPosQueue<String>()
    val chunkReceiveQueue = ChunkPosQueue<ChunkDataPacket>()
    var elapsedTicks = 0

    const val PURGE_COOLDOWN_TICKS = 1200

    fun onInitializeClient() {
        LOGGER.debug("Initializing XMMP client")
    }

    fun onTickEnd() {
        val player = Minecraft.getInstance().player ?: return
        val playerChunkPos = player.chunkPosition()

        val config = XMMPConfig.HANDLER.instance()

        sendPendingChunks(config, playerChunkPos)
        applyReceivedChunks(config, playerChunkPos)

        RegionSyncProcessor.process()

        elapsedTicks++
        if (elapsedTicks % PURGE_COOLDOWN_TICKS == 0) {
            chunkSendQueue.purgeCooldowns()
            chunkReceiveQueue.purgeCooldowns()
        }
    }

    fun onLeaveWorld() {
        chunkSendQueue.clear()
        chunkReceiveQueue.clear()
        elapsedTicks = 0

        RegionSyncProcessor.clear()
    }

    @JvmStatic
    fun onWriteChunk(chunkX: Int, chunkZ: Int) {
        val dimensionId =
            XaeroWorldMapCore.currentSession?.mapProcessor?.mapWorld?.currentDimensionId ?: return

        chunkSendQueue.add(chunkX, chunkZ, dimensionId.toString())
    }

    @JvmStatic
    fun onXaeroDimensionChanged(dimensionId: String) {
        LOGGER.debug("Xaero dimension changed: $dimensionId")

        onLeaveWorld()

        NetworkUtils.sendToServer(C2SXaeroReadyPacket(dimensionId))
    }

    fun handleChunkDataPacket(packet: ChunkDataPacket) {
        chunkReceiveQueue.add(packet.chunkX, packet.chunkZ, packet)
    }

    fun handleRegionTimestampsPacket(packet: S2CRegionTimestampsPacket) {
        LOGGER.debug("Received region timestamps from server: {}", packet.timestamps)

        for ((packed, ts) in packet.timestamps) {
            val rx = (packed shr 32).toInt()
            val rz = (packed and 0xFFFFFFFFL).toInt()

            val localTs = XaeroMapUtils.getRegionTimestamp(rx, rz)
            if (localTs != null && ts < localTs) {
                LOGGER.debug(
                    "Region ({}, {}) timestamp is newer than server (local: {}, server: {}), skipping sync",
                    rx,
                    rz,
                    localTs,
                    ts
                )
                continue
            }

            LOGGER.debug(
                "Region ({}, {}) timestamp is outdated (local: {}, server: {}), requesting sync",
                rx,
                rz,
                localTs,
                ts
            )
            RegionSyncProcessor.add(rx, rz)
        }
    }

    fun sendPendingChunks(config: XMMPConfig, playerChunkPos: ChunkPos) {
        if (chunkSendQueue.size == 0) return
        LOGGER.debug("Time to send! count: {}", chunkSendQueue.size)

        chunkSendQueue.sortByDistance(playerChunkPos.x, playerChunkPos.z)

        val currentDimensionId =
            XaeroWorldMapCore.currentSession?.mapProcessor?.mapWorld?.currentDimensionId?.toString()
                ?: return

        val requeueList = mutableListOf<ChunkPosQueue.Entry<String>>()
        var packetsSent = 0
        while (packetsSent < config.chunkSendLimit) {
            val entry = chunkSendQueue.poll() ?: break
            if (entry.data != currentDimensionId) {
                LOGGER.debug(
                    "Chunk (${entry.x}, ${entry.z}) is for dimension {} but player is in {}, ignoring",
                    entry.data,
                    currentDimensionId
                )
                continue
            }

            val packet = XaeroMapUtils.createChunkDataPacket(entry.x, entry.z)
            if (packet == null) {
                entry.retries++

                if (config.maxRetries < entry.retries) {
                    LOGGER.warn(
                        "Chunk (${entry.x}, ${entry.z}) has been requeued for sending {} times, giving up",
                        entry.retries
                    )
                } else {
                    requeueList.add(entry)
                }

                continue
            }

            LOGGER.debug("Sending chunk (${entry.x}, ${entry.z}) to server")
            NetworkUtils.sendToServer(packet)

            packetsSent++
        }

        requeueList.forEach { chunkSendQueue.add(it, ignoreCooldown = true) }
    }

    fun applyReceivedChunks(
        config: XMMPConfig,
        playerChunkPos: ChunkPos,
    ) {
        if (chunkReceiveQueue.size == 0) return
        LOGGER.debug("Time to apply! count: {}", chunkReceiveQueue.size)

        chunkReceiveQueue.sortByDistance(playerChunkPos.x, playerChunkPos.z)

        val currentDimensionId =
            XaeroWorldMapCore.currentSession?.mapProcessor?.mapWorld?.currentDimensionId?.toString()
                ?: return

        val requeueList = mutableListOf<ChunkPosQueue.Entry<ChunkDataPacket>>()
        var packetsApplied = 0
        while (packetsApplied < config.chunkApplyLimit) {
            val entry = chunkReceiveQueue.poll() ?: break
            if (entry.data.dimension != currentDimensionId) {
                LOGGER.debug(
                    "Received chunk (${entry.x}, ${entry.z}) for dimension {} but is in {}, ignoring",
                    entry.data.dimension,
                    currentDimensionId
                )
                continue
            }

            if (!XaeroMapUtils.applyChunkData(
                    entry.x,
                    entry.z,
                    entry.data.tileData,
                    entry.data.blocks
                )
            ) {
                LOGGER.debug("Failed to apply chunk (${entry.x}, ${entry.z}) received from server, will retry")

                entry.retries++

                if (config.maxRetries < entry.retries) {
                    LOGGER.warn(
                        "Chunk (${entry.x}, ${entry.z}) has been requeued for application {} times, giving up",
                        entry.retries
                    )
                } else {
                    requeueList.add(entry)
                }
            } else {
                LOGGER.debug("Applied chunk (${entry.x}, ${entry.z}) received from server")
            }

            packetsApplied++
        }

        requeueList.forEach { chunkReceiveQueue.add(it, ignoreCooldown = true) }
        LOGGER.debug(
            "Applied {} chunks received from server, {} remaining in queue, {} requeued",
            packetsApplied,
            chunkReceiveQueue.size,
            requeueList.size
        )
    }
}

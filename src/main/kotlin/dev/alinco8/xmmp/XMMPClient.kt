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

object XMMPClient {
    val chunkSendQueue = ChunkPosQueue<Unit>()
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
        chunkSendQueue.add(chunkX, chunkZ, Unit)
    }

    @JvmStatic
    fun onXaeroStateChanged(ready: Boolean) {
        if (!ready) {
            onLeaveWorld()

            return;
        }

        val level = Minecraft.getInstance().level ?: return
        NetworkUtils.sendToServer(
            C2SXaeroReadyPacket(
                level.dimension().id().toString()
            )
        )
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

        val requeueList = mutableListOf<ChunkPosQueue.Entry<Unit>>()
        var packetsSent = 0
        while (packetsSent < config.chunkSendLimit) {
            val entry = chunkSendQueue.poll() ?: break

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

        val requeueList = mutableListOf<ChunkPosQueue.Entry<ChunkDataPacket>>()
        var packetsApplied = 0
        while (packetsApplied < config.chunkApplyLimit) {
            val entry = chunkReceiveQueue.poll() ?: break

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

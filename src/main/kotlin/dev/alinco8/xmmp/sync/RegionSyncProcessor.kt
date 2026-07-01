package dev.alinco8.xmmp.sync

import dev.alinco8.xmmp.NetworkUtils
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.XMMPClient
import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.id
import dev.alinco8.xmmp.packet.C2SChunkRowRequestPacket
import java.util.concurrent.ConcurrentLinkedQueue
import net.minecraft.client.Minecraft

object RegionSyncProcessor {
    const val REGION_SIZE = 32

    private val regionRequestQueue = ConcurrentLinkedQueue<Pair<Int, Int>>()

    private var currentRegionRequest: Pair<Int, Int>? = null
    private var currentRow = 0

    fun clear() {
        regionRequestQueue.clear()
        currentRegionRequest = null
        currentRow = 0
    }

    fun add(regionX: Int, regionZ: Int) {
        regionRequestQueue.add(Pair(regionX, regionZ))
    }

    fun process() {
        val config = XMMPConfig.HANDLER.instance()

        if (XMMPClient.chunkReceiveQueue.size >= config.maxPendingSyncChunks) {
            LOGGER.debug("Receive backlog full, skipping sync processing...")

            return
        }

        if (currentRegionRequest == null || currentRow >= REGION_SIZE) {
            currentRegionRequest = regionRequestQueue.poll()
            currentRow = 0
        }

        val regionRequest = currentRegionRequest ?: return

        val level = Minecraft.getInstance().level ?: return

        LOGGER.debug(
            "Requesting chunk row: region=({}, {}), row={}",
            regionRequest.first,
            regionRequest.second,
            currentRow
        )
        NetworkUtils.sendToServer(
            C2SChunkRowRequestPacket(
                regionRequest.first,
                regionRequest.second,
                currentRow,
                Int.MAX_VALUE, //TODO: change to not re-request all chunks
                level.dimension().id().toString()
            )
        )

        currentRow++
    }
}

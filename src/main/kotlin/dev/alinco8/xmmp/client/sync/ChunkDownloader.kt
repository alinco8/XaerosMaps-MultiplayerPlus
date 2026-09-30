package dev.alinco8.xmmp.client.sync

import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.client.io.CursorStore
import dev.alinco8.xmmp.client.network.ClientPacketSender
import dev.alinco8.xmmp.network.TokenBucket
import dev.alinco8.xmmp.network.packet.C2SRegionSync
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

class ChunkDownloader(
    val dimension: ResourceKey<Level>,
    private val limiter: TokenBucket,
) {
    suspend fun onRegionIndex(
        cursorStore: CursorStore,
        regionRevisions: Map<RegionKey, Long>,
        onRequest: (RegionKey) -> Unit,
    ) {
        for ((pos, remote) in regionRevisions) {
            var cursor = cursorStore.cursor(pos)
            if (remote < cursor) {
                cursorStore.setCursor(pos, 0)
                cursor = 0L
            }
            if (remote <= cursor) continue

            limiter.waitForTokens(1.0)

            onRequest(pos)
            ClientPacketSender.sendToServer(C2SRegionSync(dimension, pos, cursor))
        }
    }
}

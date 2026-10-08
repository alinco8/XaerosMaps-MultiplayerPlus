package dev.alinco8.xmmp.client.sync

import dev.alinco8.xmmp.client.MapSyncContext
import dev.alinco8.xmmp.client.XMMPClient
import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.client.io.CursorStore
import dev.alinco8.xmmp.client.network.minecraft.MinecraftPacketSender
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.network.TokenBucket
import dev.alinco8.xmmp.core.network.packet.C2SRegionSync


class ChunkDownloader(
    val ctx: MapSyncContext,
    val dimension: ResourceId,
    private val limiter: TokenBucket,
) {
    suspend fun onRegionIndex(
        layer: SyncLayer,
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
            ctx.world.packetSender.send(
                C2SRegionSync(
                    dimension,
                    layer,
                    pos,
                    cursor
                )
            )
        }
    }
}

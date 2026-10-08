package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class S2CRegionSyncDone(
    val dimension: ResourceId,
    val layer: SyncLayer,
    val regionPos: RegionKey,
    val revision: Long,
    val syncId: Int,
) : XMMPPacket<S2CRegionSyncDone>(Companion) {
    companion object : Type<S2CRegionSyncDone>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                id, S2CRegionSyncDone::dimension,
                SyncLayer.codec, S2CRegionSyncDone::layer,
                RegionKey.codec, S2CRegionSyncDone::regionPos,
                long, S2CRegionSyncDone::revision,
                int, S2CRegionSyncDone::syncId,
                ::S2CRegionSyncDone,
            )
        }
    }
}

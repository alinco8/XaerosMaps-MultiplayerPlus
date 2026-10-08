package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class S2CRegionIndex(
    val dimension: ResourceId,
    val layer: SyncLayer,
    val regionRevisions: Map<RegionKey, Long>,
) : XMMPPacket<S2CRegionIndex>(Companion) {
    companion object : Type<S2CRegionIndex>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                id, S2CRegionIndex::dimension,
                SyncLayer.codec, S2CRegionIndex::layer,
                map(RegionKey.codec, long), S2CRegionIndex::regionRevisions,
                ::S2CRegionIndex
            )
        }
    }
}

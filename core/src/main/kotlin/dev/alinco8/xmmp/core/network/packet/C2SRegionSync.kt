package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.SyncLayer
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class C2SRegionSync(
    val dimension: ResourceId,
    val layer: SyncLayer,
    val regionPos: RegionKey,
    val cursor: Long,
) : XMMPPacket<C2SRegionSync>(Companion) {
    companion object : Type<C2SRegionSync>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                id, C2SRegionSync::dimension,
                SyncLayer.codec, C2SRegionSync::layer,
                RegionKey.codec, C2SRegionSync::regionPos,
                long, C2SRegionSync::cursor,
                ::C2SRegionSync,
            )
        }
    }
}

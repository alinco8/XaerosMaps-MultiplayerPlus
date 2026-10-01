package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.SyncLayer
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

data class C2SRegionSync(
    val dimension: ResourceKey<Level>,
    val layer: SyncLayer,
    val regionPos: RegionKey,
    val cursor: Long,
) : XMMPPacket<C2SRegionSync>(Companion) {
    companion object : Type<C2SRegionSync>(C2SRegionSync::class.java) {
        override fun id() = packetId("c2s_region_sync")

        override val codec = with(XMMPStreamCodec) {
            composite(
                resourceKey(Registries.DIMENSION), C2SRegionSync::dimension,
                SyncLayer.codec, C2SRegionSync::layer,
                RegionKey.codec, C2SRegionSync::regionPos,
                long, C2SRegionSync::cursor,
                ::C2SRegionSync,
            )
        }
    }
}

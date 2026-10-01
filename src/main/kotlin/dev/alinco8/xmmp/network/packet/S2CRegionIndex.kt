package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.SyncLayer
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

data class S2CRegionIndex(
    val dimension: ResourceKey<Level>,
    val layer: SyncLayer,
    val regionRevisions: Map<RegionKey, Long>,
) : XMMPPacket<S2CRegionIndex>(Companion) {
    companion object : Type<S2CRegionIndex>(S2CRegionIndex::class.java) {
        override fun id() = packetId("s2c_region_index")

        override val codec = with(XMMPStreamCodec) {
            composite(
                resourceKey(Registries.DIMENSION), S2CRegionIndex::dimension,
                SyncLayer.codec, S2CRegionIndex::layer,
                map(RegionKey.codec, long), S2CRegionIndex::regionRevisions,
                ::S2CRegionIndex
            )
        }
    }
}

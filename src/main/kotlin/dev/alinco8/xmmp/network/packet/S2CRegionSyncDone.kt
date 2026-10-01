package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.SyncLayer
import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

data class S2CRegionSyncDone(
    val dimension: ResourceKey<Level>,
    val layer: SyncLayer,
    val regionPos: RegionKey,
    val revision: Long,
    val syncId: Int,
) : XMMPPacket<S2CRegionSyncDone>(Companion) {
    companion object : Type<S2CRegionSyncDone>(S2CRegionSyncDone::class.java) {
        override fun id() = packetId("s2c_region_sync_done")

        override val codec = with(XMMPStreamCodec) {
            composite(
                resourceKey(Registries.DIMENSION), S2CRegionSyncDone::dimension,
                SyncLayer.codec, S2CRegionSyncDone::layer,
                RegionKey.codec, S2CRegionSyncDone::regionPos,
                long, S2CRegionSyncDone::revision,
                int, S2CRegionSyncDone::syncId,
                ::S2CRegionSyncDone,
            )
        }
    }
}

package dev.alinco8.xmmp.network.packet

import dev.alinco8.xmmp.network.XMMPPacket
import dev.alinco8.xmmp.network.XMMPStreamCodec
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

data class C2SDimensionSync(
    val dimension: ResourceKey<Level>,
    val syncId: Int,
) : XMMPPacket<C2SDimensionSync>(Companion) {

    companion object : Type<C2SDimensionSync>(C2SDimensionSync::class.java) {

        override fun id() = packetId("c2s_dimension_sync")

        override val codec = with(XMMPStreamCodec) {
            composite(
                resourceKey(Registries.DIMENSION), C2SDimensionSync::dimension,
                int, C2SDimensionSync::syncId,
                ::C2SDimensionSync
            )
        }
    }
}

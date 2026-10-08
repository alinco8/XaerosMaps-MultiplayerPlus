package dev.alinco8.xmmp.core.network.packet

import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class C2SDimensionSync(
    val dimension: ResourceId,
    val syncId: Int,
) : XMMPPacket<C2SDimensionSync>(Companion) {
    companion object : Type<C2SDimensionSync>() {
        override val codec = with(XMMPStreamCodec) {
            composite(
                id, C2SDimensionSync::dimension,
                int, C2SDimensionSync::syncId,
                ::C2SDimensionSync
            )
        }
    }
}

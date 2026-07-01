package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.loc
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class S2CRegionTimestampsPacket(
    val timestamps: Map<Long, Long>,
    val dimension: String,
) : CustomPacketPayload {
    companion object {
        val TYPE = CustomPacketPayload.Type<S2CRegionTimestampsPacket>(loc("s2c_region_timestamps"))
        val STREAM_CODEC = StreamCodec.of<FriendlyByteBuf, S2CRegionTimestampsPacket>(
            { buf, packet ->
                buf.writeInt(packet.timestamps.size)
                packet.timestamps.forEach { (key, value) ->
                    buf.writeLong(key)
                    buf.writeLong(value)
                }
                buf.writeUtf(packet.dimension)
            },
            { buf ->
                val size = buf.readInt()
                S2CRegionTimestampsPacket(
                    buildMap(size) {
                        repeat(size) {
                            val key = buf.readLong()
                            val value = buf.readLong()
                            put(key, value)
                        }
                    },
                    buf.readUtf()
                )
            }
        )
    }

    override fun type() = TYPE
}

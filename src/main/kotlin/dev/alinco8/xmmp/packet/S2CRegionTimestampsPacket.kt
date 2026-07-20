package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.common.XMMPPacket
import dev.alinco8.xmmp.common.XMMPPacketType
import dev.alinco8.xmmp.loc
import net.minecraft.network.FriendlyByteBuf

data class S2CRegionTimestampsPacket(
    val timestamps: Map<Long, Long>,
    val dimension: String,
) : XMMPPacket<S2CRegionTimestampsPacket>() {
    companion object : XMMPPacketType<S2CRegionTimestampsPacket>() {
        override fun id() = loc("s2c_region_timestamps")

        override fun decode(buf: FriendlyByteBuf): S2CRegionTimestampsPacket {
            val size = buf.readInt()

            return S2CRegionTimestampsPacket(
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

        override fun encode(buf: FriendlyByteBuf, packet: S2CRegionTimestampsPacket) {
            buf.writeInt(packet.timestamps.size)
            packet.timestamps.forEach { (key, value) ->
                buf.writeLong(key)
                buf.writeLong(value)
            }
            buf.writeUtf(packet.dimension)
        }
    }

    override val encode = Companion::encode
    override val payloadType = Companion.payloadType
}

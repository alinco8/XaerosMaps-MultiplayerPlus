package dev.alinco8.xmmp.packet

import dev.alinco8.xmmp.loc
import net.minecraft.network.FriendlyByteBuf

//? if >= 1.20.5 {
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

//? }

data class S2CRegionTimestampsPacket(
    val timestamps: Map<Long, Long>,
    val dimension: String,
)/*?>=1.20.5>>'{'*/ : CustomPacketPayload   {
    //? if >=1.20.5 {
    companion object {
        val TYPE = CustomPacketPayload.Type<S2CRegionTimestampsPacket>(loc("s2c_region_timestamps"))!!
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
    //? } else {
    /*companion object {
        fun decode(buf: FriendlyByteBuf): S2CRegionTimestampsPacket {
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
    }

    fun encode(buf: FriendlyByteBuf) {
        buf.writeInt(timestamps.size)
        timestamps.forEach { (key, value) ->
            buf.writeLong(key)
            buf.writeLong(value)
        }
        buf.writeUtf(dimension)
    }
    *///? }
}

package dev.alinco8.xmmp.network

//? if >=1.20.5 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
//? } else if fabric {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket
import net.fabricmc.fabric.api.networking.v1.PacketType
*///? }

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier

*///? } else {
import net.minecraft.resources.ResourceLocation as Identifier

//? }

import dev.alinco8.xmmp.XMMP
import net.minecraft.network.FriendlyByteBuf

abstract class XMMPPacket<T : XMMPPacket<T>>(val type: Type<T>)
/*? if >=1.20.5 {*/
    : CustomPacketPayload
/*?} else if fabric{*/
/*: FabricPacket
*//*?}*/ {
    abstract class Type<T : XMMPPacket<T>>(
        val packet: Class<T>,
    ) {
        companion object {
            fun packetId(id: String) = XMMP.loc("${id}_v${XMMP.PACKET_VERSION}")
        }

        abstract fun id(): Identifier
        abstract val codec: XMMPStreamCodec<T>

        //? if >=1.20.5 {
        val payloadType by lazy { CustomPacketPayload.Type<T>(id()) }
        //? } else if fabric {
        /*val payloadType by lazy { PacketType.create(id(), codec::decode) }
        *///? }
    }

    //? if >=1.20.5 {
    override fun type() = CustomPacketPayload.Type<T>(type.id())
    //? } else if fabric {
    /*@Suppress("UNCHECKED_CAST")
    override fun write(buf: FriendlyByteBuf) = type.codec.encode(buf, this as T)
    override fun getType(): PacketType<T> = PacketType.create(type.id(), type.codec::decode)
    *///? }
}

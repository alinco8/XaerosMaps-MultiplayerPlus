package dev.alinco8.xmmp.network

//? if >=1.20.5 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
//? } else if fabric {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket
import net.fabricmc.fabric.api.networking.v1.PacketType
import net.minecraft.network.FriendlyByteBuf
*///? }


import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.core.network.XMMPStreamCodec

data class FramePacket(
    val frame: ByteArray,
)
/*? if >=1.20.5 {*/
    : CustomPacketPayload
/*?} else if fabric{*/
/*: FabricPacket
*//*?}*/ {
    companion object {
        val ID = XMMP.loc("frame_v${XMMP.PACKET_VERSION}")
        val CODEC = with(XMMPStreamCodec) {
            composite(
                byteArray, FramePacket::frame,
                ::FramePacket
            )
        }

        //? if >=1.20.5 {
        val payloadType by lazy { CustomPacketPayload.Type<FramePacket>(ID) }
        //? } else if fabric {
        /*val payloadType by lazy { PacketType.create(ID, CODEC::decode) }
        *///? }
    }

    //? if >=1.20.5 {
    override fun type() = payloadType
    //? } else if fabric {
    /*@Suppress("UNCHECKED_CAST")
    override fun write(buf: FriendlyByteBuf) = CODEC.encode(buf, this)
    override fun getType(): PacketType<FramePacket> = payloadType
    *///? }

    override fun hashCode() = frame.contentHashCode()
    override fun equals(other: Any?) = other is FramePacket && frame.contentEquals(other.frame)
}

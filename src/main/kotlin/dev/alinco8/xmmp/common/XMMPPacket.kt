package dev.alinco8.xmmp.common

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

import net.minecraft.network.FriendlyByteBuf

abstract class XMMPPacketType<T : XMMPPacket<T>> {
    abstract fun id(): Identifier
    abstract fun decode(buf: FriendlyByteBuf): T
    abstract fun encode(buf: FriendlyByteBuf, packet: T)

    //? if >=1.20.5 {
    val payloadType: CustomPacketPayload.Type<T> by lazy { CustomPacketPayload.Type(id()) }
    //? } else if fabric {
    /*val payloadType: PacketType<T> = PacketType.create(id(), ::decode)
    *///? } else {
    /*val payloadType = null
    *///? }
}

abstract class XMMPPacket<T : XMMPPacket<T>>
//? if >=1.20.5 {
    : CustomPacketPayload
//? } else if fabric {
/*: FabricPacket
*///? }
{
    abstract val encode: (buf: FriendlyByteBuf, packet: T) -> Unit

    //? if >=1.20.5 {
    abstract val payloadType: CustomPacketPayload.Type<T>
    //? } else if fabric {
    /*abstract val payloadType: PacketType<T>
    *///? } else {
    /*abstract val payloadType: Nothing?
    *///? }

    //? if >=1.20.5 {
    override fun type() = payloadType
    //? } else if fabric {
    /*override fun write(buf: FriendlyByteBuf) = encode(buf, this as T)
    override fun getType() = payloadType
    *///? }
}

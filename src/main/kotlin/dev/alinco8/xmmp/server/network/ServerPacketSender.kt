package dev.alinco8.xmmp.server.network

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier
*///? } else {
import net.minecraft.resources.ResourceLocation as Identifier
//? }

//? if fabric {
/*import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

*///? } else if neoforge {
import net.neoforged.neoforge.network.PacketDistributor

//? } else if forge {
/*import dev.alinco8.xmmp.platform.forge.ForgeEntrypoint
import net.minecraftforge.network.PacketDistributor

*///? }

import dev.alinco8.xmmp.core.network.XMMPPacket
import dev.alinco8.xmmp.core.network.writeId
import dev.alinco8.xmmp.network.FramePacket
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import net.minecraft.server.level.ServerPlayer

object ServerPacketSender {
    fun <T : XMMPPacket<T>> sendToPlayer(
        player: ServerPlayer,
        packet: T,
    ): Boolean {
        //? if fabric {
        /*if (!ServerPlayNetworking.canSend(player, FramePacket.payloadType)) return false

        val buf = Unpooled.buffer()
        buf.writeId(packet.type.id)
        packet.type.codec.encode(buf, packet)

        ServerPlayNetworking.send(player, FramePacket(ByteBufUtil.getBytes(buf)))

        *///? } else if neoforge {
        if (!player.connection.hasChannel(FramePacket.payloadType)) return false

        val buf = Unpooled.buffer()
        buf.writeId(packet.type.id)
        packet.type.codec.encode(buf, packet)

        PacketDistributor.sendToPlayer(player, FramePacket(ByteBufUtil.getBytes(buf)))

        //? } else if forge {
        /*if (!ForgeEntrypoint.CHANNEL.isRemotePresent(player.connection.connection)) return false

        val buf = Unpooled.buffer()
        buf.writeId(packet.type.id)
        packet.type.codec.encode(buf, packet)

        ForgeEntrypoint.CHANNEL.send(
            PacketDistributor.PLAYER.with { player }, FramePacket(
                ByteBufUtil.getBytes(buf)
            )
        )

        *///? }

        return true
    }
}

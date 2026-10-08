package dev.alinco8.xmmp.standalone

import dev.alinco8.xmmp.core.network.writeVarInt
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class VarIntFrameEncoder : MessageToByteEncoder<ByteBuf>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: ByteBuf,
        out: ByteBuf,
    ) {
        out.writeVarInt(msg.readableBytes())
        out.writeBytes(msg, msg.readerIndex(), msg.readableBytes())
    }
}

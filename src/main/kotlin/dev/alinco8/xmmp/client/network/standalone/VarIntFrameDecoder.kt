package dev.alinco8.xmmp.client.network.standalone

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.CorruptedFrameException

class VarIntFrameDecoder(
    private val maxFrameSize: Int = 16 * 1024 * 1024,
) : ByteToMessageDecoder() {
    override fun decode(
        ctx: ChannelHandlerContext,
        input: ByteBuf,
        output: MutableList<Any>,
    ) {
        val startIdx = input.readerIndex()

        var len = 0
        var shift = 0
        var varIntBytes = 0

        while (true) {
            if (!input.isReadable) {
                input.readerIndex(startIdx)
                return
            }

            val byte = input.readUnsignedByte().toInt()
            varIntBytes++

            len = len or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) {
                break
            }

            shift += 7

            if (varIntBytes >= 5) {
                throw CorruptedFrameException("VarInt is too big")
            }
        }

        if (len !in 0..maxFrameSize) {
            throw CorruptedFrameException("Invalid frame length: $len (max: $maxFrameSize)")
        }
        if (input.readableBytes() < len) {
            input.readerIndex(startIdx)
            return
        }

        output += input.readRetainedSlice(len)
    }
}

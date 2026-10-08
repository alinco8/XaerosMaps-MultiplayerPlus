package dev.alinco8.xmmp.core.network

import dev.alinco8.xmmp.core.ResourceId
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil

fun ByteBuf.readVarInt(): Int {
    var out = 0
    var shift = 0
    while (true) {
        val b = readByte().toInt()
        out = out or ((b and 0x7F) shl shift)
        if ((b and 0x80) == 0) return out
        shift += 7
        if (shift > 28) error("VarInt too big")
    }
}

fun ByteBuf.writeVarInt(value: Int): ByteBuf {
    var v = value
    while ((v and 0x7F.inv()) != 0) {
        writeByte((v and 0x7F) or 0x80)
        v = v ushr 7
    }
    return writeByte(v)
}


fun ByteBuf.readByteArray(maxSize: Int = readableBytes()): ByteArray {
    val i = readVarInt()
    require(i in 0..maxSize) { "Byte array size $i is out of bounds (max $maxSize)" }

    val bytes = ByteArray(i)
    readBytes(bytes)

    return bytes
}

fun ByteBuf.writeByteArray(bytes: ByteArray): ByteBuf {
    writeVarInt(bytes.size)
    return writeBytes(bytes)
}

fun ByteBuf.readLongArray(
    array: LongArray? = null,
    maxSize: Int = readableBytes() / Long.SIZE_BYTES,
): LongArray {
    val i = readVarInt()
    require(i in 0..maxSize) { "Long array size $i is out of bounds (max $maxSize)" }

    val array = if (array == null || array.size != i) {

        LongArray(i)
    } else array

    for (j in 0 until i) {
        array[j] = readLong()
    }

    return array
}

fun ByteBuf.writeLongArray(array: LongArray): ByteBuf {
    writeVarInt(array.size)
    for (l in array) {
        writeLong(l)
    }

    return this
}

fun ByteBuf.readUtf(maxLen: Int = Short.MAX_VALUE.toInt()): String {
    val maxEncodedLen = ByteBufUtil.utf8MaxBytes(maxLen)
    val bufLen = readVarInt()
    require(bufLen in 0..maxEncodedLen) { "UTF-8 string length $bufLen is out of bounds (max $maxEncodedLen)" }

    val available = readableBytes()
    require(bufLen <= available) { "Not enough bytes in buffer, expected $bufLen, but got $available" }

    val result = toString(readerIndex(), bufLen, Charsets.UTF_8)
    readerIndex(readerIndex() + bufLen)
    require(result.length <= maxLen) { "UTF-8 string length ${result.length} is out of bounds (max $maxLen)" }

    return result
}

fun ByteBuf.writeUtf(value: CharSequence, maxLen: Int = Short.MAX_VALUE.toInt()) {
    require(value.length <= maxLen) { "UTF-8 string length ${value.length} is out of bounds (max $maxLen)" }

    val tmp = alloc().buffer(ByteBufUtil.utf8MaxBytes(value))

    try {
        val bytesWritten = ByteBufUtil.writeUtf8(tmp, value)
        require(bytesWritten <= ByteBufUtil.utf8MaxBytes(maxLen)) {
            "UTF-8 string length $bytesWritten is out of bounds (max ${
                ByteBufUtil.utf8MaxBytes(
                    maxLen
                )
            })"
        }

        writeVarInt(bytesWritten)
        writeBytes(tmp)
    } finally {
        tmp.release()
    }
}

fun ByteBuf.readId() = ResourceId(readUtf(), readUtf())

fun ByteBuf.writeId(id: ResourceId) {
    writeUtf(id.namespace)
    writeUtf(id.path)
}

fun <T> ByteBuf.use(block: (ByteBuf) -> T): T {
    return try {
        block(this)
    } finally {
        release()
    }
}

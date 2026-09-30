package dev.alinco8.xmmp.io

object PayloadHash {
    fun of(bytes: ByteArray): Long {
        var h = -0x340d631b7bdddcdbL
        for (b in bytes) {
            h = h xor (b.toLong() and 0xff)
            h *= 0x100000001b3L
        }

        return if (h == 0L) 1L else h
    }
}

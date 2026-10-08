package dev.alinco8.xmmp.core.network

import io.netty.buffer.ByteBuf

interface XMMPStreamCodec<T> {
    fun decode(buf: ByteBuf): T
    fun encode(buf: ByteBuf, value: T)

    @Suppress("TooManyFunctions", "LongParameterList")
    companion object {
        fun <T> of(
            decode: (ByteBuf) -> T,
            encode: (ByteBuf, T) -> Unit,
        ): XMMPStreamCodec<T> = object : XMMPStreamCodec<T> {
            override fun decode(buf: ByteBuf) = decode(buf)
            override fun encode(buf: ByteBuf, value: T) = encode(buf, value)
        }

        fun <C, T1> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            constructor: (T1) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
            }
        )

        fun <C, T1, T2> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            codec2: XMMPStreamCodec<T2>, getter2: (C) -> T2,
            constructor: (T1, T2) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                    codec2.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
                codec2.encode(buf, getter2(value))
            }
        )

        fun <C, T1, T2, T3> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            codec2: XMMPStreamCodec<T2>, getter2: (C) -> T2,
            codec3: XMMPStreamCodec<T3>, getter3: (C) -> T3,
            constructor: (T1, T2, T3) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                    codec2.decode(buf),
                    codec3.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
                codec2.encode(buf, getter2(value))
                codec3.encode(buf, getter3(value))
            }
        )

        fun <C, T1, T2, T3, T4> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            codec2: XMMPStreamCodec<T2>, getter2: (C) -> T2,
            codec3: XMMPStreamCodec<T3>, getter3: (C) -> T3,
            codec4: XMMPStreamCodec<T4>, getter4: (C) -> T4,
            constructor: (T1, T2, T3, T4) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                    codec2.decode(buf),
                    codec3.decode(buf),
                    codec4.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
                codec2.encode(buf, getter2(value))
                codec3.encode(buf, getter3(value))
                codec4.encode(buf, getter4(value))
            }
        )

        fun <C, T1, T2, T3, T4, T5> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            codec2: XMMPStreamCodec<T2>, getter2: (C) -> T2,
            codec3: XMMPStreamCodec<T3>, getter3: (C) -> T3,
            codec4: XMMPStreamCodec<T4>, getter4: (C) -> T4,
            codec5: XMMPStreamCodec<T5>, getter5: (C) -> T5,
            constructor: (T1, T2, T3, T4, T5) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                    codec2.decode(buf),
                    codec3.decode(buf),
                    codec4.decode(buf),
                    codec5.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
                codec2.encode(buf, getter2(value))
                codec3.encode(buf, getter3(value))
                codec4.encode(buf, getter4(value))
                codec5.encode(buf, getter5(value))
            }
        )

        fun <C, T1, T2, T3, T4, T5, T6> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            codec2: XMMPStreamCodec<T2>, getter2: (C) -> T2,
            codec3: XMMPStreamCodec<T3>, getter3: (C) -> T3,
            codec4: XMMPStreamCodec<T4>, getter4: (C) -> T4,
            codec5: XMMPStreamCodec<T5>, getter5: (C) -> T5,
            codec6: XMMPStreamCodec<T6>, getter6: (C) -> T6,
            constructor: (T1, T2, T3, T4, T5, T6) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                    codec2.decode(buf),
                    codec3.decode(buf),
                    codec4.decode(buf),
                    codec5.decode(buf),
                    codec6.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
                codec2.encode(buf, getter2(value))
                codec3.encode(buf, getter3(value))
                codec4.encode(buf, getter4(value))
                codec5.encode(buf, getter5(value))
                codec6.encode(buf, getter6(value))
            }
        )

        fun <C, T1, T2, T3, T4, T5, T6, T7> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            codec2: XMMPStreamCodec<T2>, getter2: (C) -> T2,
            codec3: XMMPStreamCodec<T3>, getter3: (C) -> T3,
            codec4: XMMPStreamCodec<T4>, getter4: (C) -> T4,
            codec5: XMMPStreamCodec<T5>, getter5: (C) -> T5,
            codec6: XMMPStreamCodec<T6>, getter6: (C) -> T6,
            codec7: XMMPStreamCodec<T7>, getter7: (C) -> T7,
            constructor: (T1, T2, T3, T4, T5, T6, T7) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                    codec2.decode(buf),
                    codec3.decode(buf),
                    codec4.decode(buf),
                    codec5.decode(buf),
                    codec6.decode(buf),
                    codec7.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
                codec2.encode(buf, getter2(value))
                codec3.encode(buf, getter3(value))
                codec4.encode(buf, getter4(value))
                codec5.encode(buf, getter5(value))
                codec6.encode(buf, getter6(value))
                codec7.encode(buf, getter7(value))
            }
        )

        fun <C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> composite(
            codec1: XMMPStreamCodec<T1>, getter1: (C) -> T1,
            codec2: XMMPStreamCodec<T2>, getter2: (C) -> T2,
            codec3: XMMPStreamCodec<T3>, getter3: (C) -> T3,
            codec4: XMMPStreamCodec<T4>, getter4: (C) -> T4,
            codec5: XMMPStreamCodec<T5>, getter5: (C) -> T5,
            codec6: XMMPStreamCodec<T6>, getter6: (C) -> T6,
            codec7: XMMPStreamCodec<T7>, getter7: (C) -> T7,
            codec8: XMMPStreamCodec<T8>, getter8: (C) -> T8,
            codec9: XMMPStreamCodec<T9>, getter9: (C) -> T9,
            codec10: XMMPStreamCodec<T10>, getter10: (C) -> T10,
            codec11: XMMPStreamCodec<T11>, getter11: (C) -> T11,
            constructor: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) -> C,
        ) = of(
            { buf ->
                constructor(
                    codec1.decode(buf),
                    codec2.decode(buf),
                    codec3.decode(buf),
                    codec4.decode(buf),
                    codec5.decode(buf),
                    codec6.decode(buf),
                    codec7.decode(buf),
                    codec8.decode(buf),
                    codec9.decode(buf),
                    codec10.decode(buf),
                    codec11.decode(buf),
                )
            },
            { buf, value ->
                codec1.encode(buf, getter1(value))
                codec2.encode(buf, getter2(value))
                codec3.encode(buf, getter3(value))
                codec4.encode(buf, getter4(value))
                codec5.encode(buf, getter5(value))
                codec6.encode(buf, getter6(value))
                codec7.encode(buf, getter7(value))
                codec8.encode(buf, getter8(value))
                codec9.encode(buf, getter9(value))
                codec10.encode(buf, getter10(value))
                codec11.encode(buf, getter11(value))
            }
        )

        val short = of(
            { buf -> buf.readShort() },
            { buf, value -> buf.writeShort(value.toInt()) },
        )

        val int = of(
            { buf -> buf.readInt() },
            { buf, value -> buf.writeInt(value) },
        )

        val long = of(
            { buf -> buf.readLong() },
            { buf, value -> buf.writeLong(value) },
        )

        val double = of(
            { buf -> buf.readDouble() },
            { buf, value -> buf.writeDouble(value) },
        )

        val byte = of(
            { buf -> buf.readByte() },
            { buf, value -> buf.writeByte(value.toInt()) },
        )

        val byteArray = of(
            { buf -> buf.readByteArray() },
            { buf, value -> buf.writeByteArray(value) },
        )

        val boolean = of(
            { buf -> buf.readBoolean() },
            { buf, value -> buf.writeBoolean(value) },
        )

        val id = of(
            { buf -> buf.readId() },
            { buf, value -> buf.writeId(value) },
        )

        fun <T> list(elementCodec: XMMPStreamCodec<T>, maxSize: Int) = of(
            { buf ->
                val size = buf.readInt()
                require(size <= maxSize) { "List size $size exceeds maximum size $maxSize" }
                List(size) { elementCodec.decode(buf) }
            },
            { buf, value ->
                buf.writeInt(value.size)
                value.forEach { elementCodec.encode(buf, it) }
            }
        )

        fun <K, V> map(
            keyCodec: XMMPStreamCodec<K>,
            valueCodec: XMMPStreamCodec<V>,
        ) = of(
            { buf ->
                val size = buf.readInt()

                buildMap {
                    repeat(size) {
                        put(
                            keyCodec.decode(buf),
                            valueCodec.decode(buf),
                        )
                    }
                }
            },
            { buf, value ->
                buf.writeInt(value.size)
                value.forEach { (key, v) ->
                    keyCodec.encode(buf, key)
                    valueCodec.encode(buf, v)
                }
            }
        )

        fun <L, R> pair(
            leftCodec: XMMPStreamCodec<L>,
            rightCodec: XMMPStreamCodec<R>,
        ) = of(
            { buf ->
                leftCodec.decode(buf) to rightCodec.decode(buf)
            },
            { buf, value ->
                leftCodec.encode(buf, value.first)
                rightCodec.encode(buf, value.second)
            }
        )
    }
}

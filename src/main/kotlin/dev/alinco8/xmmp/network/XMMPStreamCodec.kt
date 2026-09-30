package dev.alinco8.xmmp.network

import net.minecraft.core.Registry
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey

interface XMMPStreamCodec<T> {
    fun decode(buf: FriendlyByteBuf): T
    fun encode(buf: FriendlyByteBuf, value: T)

    @Suppress("TooManyFunctions")
    companion object {
        fun <T> of(
            decode: (FriendlyByteBuf) -> T,
            encode: (FriendlyByteBuf, T) -> Unit,
        ): XMMPStreamCodec<T> = object : XMMPStreamCodec<T> {
            override fun decode(buf: FriendlyByteBuf) = decode(buf)
            override fun encode(buf: FriendlyByteBuf, value: T) = encode(buf, value)
        }

        @Suppress("LongParameterList")
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

        @Suppress("LongParameterList")
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

        @Suppress("LongParameterList")
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

        @Suppress("LongParameterList")
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

        @Suppress("LongParameterList")
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

        @Suppress("LongParameterList")
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

        val longArray = of(
            { buf -> buf.readLongArray() },
            { buf, value -> buf.writeLongArray(value) },
        )

        val boolean = of(
            { buf -> buf.readBoolean() },
            { buf, value -> buf.writeBoolean(value) },
        )

        fun <T : Any> resourceKey(registryKey: ResourceKey<Registry<T>>) = of(
            { buf -> buf.readResourceKey(registryKey) },
            { buf, value -> buf.writeResourceKey(value) },
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

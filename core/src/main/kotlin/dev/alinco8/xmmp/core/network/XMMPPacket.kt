package dev.alinco8.xmmp.core.network

import dev.alinco8.xmmp.core.XMMPShared

abstract class XMMPPacket<T : XMMPPacket<T>>(val type: Type<T>) {
    companion object {
        fun packetId(id: String) = XMMPShared.id(id)
    }

    abstract class Type<T : XMMPPacket<T>> {
        val id = packetId(
            javaClass.enclosingClass.simpleName.pascalToSnakeCase()
        )

        abstract val codec: XMMPStreamCodec<T>
    }
}

private fun String.pascalToSnakeCase() =
    replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2")
        .replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .lowercase()

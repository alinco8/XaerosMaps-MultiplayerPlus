package dev.alinco8.xmmp.common

interface ClientEvents {
    fun registerWorldJoin(callback: () -> Unit)
    fun registerWorldLeave(callback: () -> Unit)

    fun registerTickPost(callback: () -> Unit)
}

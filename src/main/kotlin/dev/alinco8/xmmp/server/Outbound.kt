package dev.alinco8.xmmp.server

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.RegionKey
import kotlinx.coroutines.channels.Channel

class Outbound {
    sealed interface Item
    data class Chunk(val pos: ChunkKey) : Item
    data class Done(val region: RegionKey, val revision: Long, val syncId: Int) : Item

    private val items = LinkedHashMap<Any, Item>()
    private val wakeup = Channel<Unit>(Channel.CONFLATED)

    fun enqueueChunk(pos: ChunkKey) {
        synchronized(items) { items.putIfAbsent(pos, Chunk(pos)) }
        wakeup.trySend(Unit)
    }

    fun enqueueDone(region: RegionKey, revision: Long, syncId: Int) {
        val done = Done(region, revision, syncId)
        synchronized(items) { items[done] = done }
        wakeup.trySend(Unit)
    }

    fun poll(): Item? = synchronized(items) {
        val it = items.values.iterator()
        if (it.hasNext()) it.next().also { _ -> it.remove() } else null
    }

    fun size() = synchronized(items) { items.size }
    fun clear() = synchronized(items) { items.clear() }

    fun signal() = wakeup.trySend(Unit)
    suspend fun awaitSignal() = wakeup.receive()
}

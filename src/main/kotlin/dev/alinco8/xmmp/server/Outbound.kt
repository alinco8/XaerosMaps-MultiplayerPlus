package dev.alinco8.xmmp.server

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.SyncLayer
import kotlinx.coroutines.channels.Channel

class Outbound {
    sealed interface Item
    data class Chunk(val layer: SyncLayer, val pos: ChunkKey) : Item
    data class Done(
        val layer: SyncLayer,
        val region: RegionKey,
        val revision: Long,
        val syncId: Int,
    ) : Item

    private val items = LinkedHashSet<Item>()
    private val wakeup = Channel<Unit>(Channel.CONFLATED)

    fun enqueueChunk(layer: SyncLayer, pos: ChunkKey) {
        synchronized(items) {
            items.add(Chunk(layer, pos))
        }
        wakeup.trySend(Unit)
    }

    fun enqueueDone(layer: SyncLayer, region: RegionKey, revision: Long, syncId: Int) {
        val done = Done(layer, region, revision, syncId)
        synchronized(items) { items.add(done) }
        wakeup.trySend(Unit)
    }

    fun poll(): Item? = synchronized(items) {
        val it = items.iterator()
        if (it.hasNext()) it.next().also { _ -> it.remove() } else null
    }

    fun size() = synchronized(items) { items.size }
    fun clear() = synchronized(items) { items.clear() }

    fun signal() = wakeup.trySend(Unit)
    suspend fun awaitSignal() = wakeup.receive()
}

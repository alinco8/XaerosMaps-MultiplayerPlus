package dev.alinco8.xmmp.client.sync

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.client.network.ClientPacketSender
import dev.alinco8.xmmp.SyncLayer
import dev.alinco8.xmmp.network.packet.C2SChunkUpload
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

class ChunkUploader(
    val dimension: ResourceKey<Level>,
    val layer: SyncLayer,
    private val flow: UploadFlow,
) {
    private class Pending(val payload: ByteArray, val hash: Long)

    companion object {
        private const val MAX_PENDING_CHUNKS = 10_000
    }

    private val pending = LinkedHashMap<ChunkKey, Pending>()
    private val sentHashes = HashMap<ChunkKey, Long>()

    @Synchronized
    fun pendingCount() = pending.size

    @Synchronized
    private fun markSent(key: ChunkKey, hash: Long) {
        sentHashes[key] = hash
    }

    @Synchronized
    private fun takeNext(): Triple<Long, ChunkKey, ByteArray>? {
        if (pending.isEmpty()) return null
        val e = pending.entries.first()
        val key = e.key
        val hash = e.value.hash
        val seq = flow.tryAcquire { markSent(key, hash) } ?: return null
        pending.remove(key)

        return Triple(seq, key, e.value.payload)
    }

    @Synchronized
    fun offer(key: ChunkKey, payload: ByteArray, hash: Long) {
        if (sentHashes[key] == hash) {
            pending.remove(key)
            return
        }
        pending[key] = Pending(payload, hash)

        if (MAX_PENDING_CHUNKS < pending.size) {
            pending.entries.iterator().run {
                next()
                remove()
            }
        }
    }

    @Synchronized
    private fun hasPending() = pending.isNotEmpty()

    suspend fun tick() {
        while (hasPending() && flow.hasCredit()) {
            flow.limiter.waitForTokens(1.0)

            val (seq, key, payload) = takeNext() ?: break
            ClientPacketSender.sendToServer(
                C2SChunkUpload(dimension, layer, key, seq, payload)
            )
        }
    }
}

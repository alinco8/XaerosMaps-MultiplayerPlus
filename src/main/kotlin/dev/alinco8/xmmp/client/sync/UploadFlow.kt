package dev.alinco8.xmmp.client.sync

import dev.alinco8.xmmp.config.ServerConfig
import dev.alinco8.xmmp.network.CreditWindow
import dev.alinco8.xmmp.network.TokenBucket

class UploadFlow(serverConfig: ServerConfig) {
    private class Entry(val seq: Long, val onAcked: () -> Unit)

    private val credits = CreditWindow(serverConfig.uploadWindow)
    val limiter = TokenBucket(
        serverConfig.uploadBurst,
        serverConfig.uploadRateLimit * 0.9
    )

    private val inFlight = ArrayDeque<Entry>()
    private var lastSeq = 0L

    fun hasCredit() = credits.available > 0
    val inFlightCount get() = credits.inFlightCount

    @Synchronized
    fun tryAcquire(onAcked: () -> Unit): Long? {
        if (!credits.tryAcquire()) return null
        val seq = ++lastSeq
        inFlight.addLast(Entry(seq, onAcked))
        return seq
    }

    fun onAck(lastSeq: Long) {
        val acked = ArrayList<Entry>()
        synchronized(this) {
            while (inFlight.isNotEmpty() && inFlight.first().seq <= lastSeq) {
                acked.add(inFlight.removeFirst())
            }
            credits.release(acked.size)
        }
        acked.forEach { it.onAcked() }
    }
}

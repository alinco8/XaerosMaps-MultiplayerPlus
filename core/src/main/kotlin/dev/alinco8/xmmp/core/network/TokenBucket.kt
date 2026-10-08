package dev.alinco8.xmmp.core.network

import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

class TokenBucket(
    private val capacity: Double,
    private val refillPerSecond: Double,
) {
    companion object {
        const val ONE_SECOND_NANOS = 1_000_000_000.0
    }

    private var tokens: Double = capacity
    private var lastRefillNanos: Long = System.nanoTime()

    @Synchronized
    fun tryConsume(cost: Double): Boolean {
        refillTokens()
        return if (tokens >= cost) {
            tokens -= cost
            true
        } else {
            false
        }
    }

    suspend fun waitForTokens(cost: Double) {
        require(cost <= capacity) { "Cost cannot exceed bucket capacity" }

        while (true) {
            if (tryConsume(cost)) return
            val waitTime = ceil((cost - tokens) / refillPerSecond * 1000).toLong()
            delay(waitTime.milliseconds)
        }
    }

    @Synchronized
    private fun refillTokens() {
        val now = System.nanoTime()
        val elapsedSeconds = (now - lastRefillNanos) / ONE_SECOND_NANOS
        tokens = minOf(capacity, tokens + elapsedSeconds * refillPerSecond)
        lastRefillNanos = now
    }
}

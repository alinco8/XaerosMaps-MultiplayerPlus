package dev.alinco8.xmmp.network

class CreditWindow(size: Int) {
    private val size = size.coerceAtLeast(1)
    private var inFlight = 0

    val available: Int @Synchronized get() = size - inFlight
    val inFlightCount: Int @Synchronized get() = inFlight

    @Synchronized
    fun tryAcquire(): Boolean {
        if (inFlight >= size) return false
        inFlight++

        return true
    }

    @Synchronized
    fun release(n: Int) {
        if (n <= 0) return
        inFlight = (inFlight - n).coerceAtLeast(0)
    }

    @Synchronized
    fun reset() {
        inFlight = 0
    }
}

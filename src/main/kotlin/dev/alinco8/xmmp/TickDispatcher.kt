package dev.alinco8.xmmp

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable

class TickDispatcher : CoroutineDispatcher() {
    private val queue = ConcurrentLinkedQueue<Runnable>()
    @Volatile
    private var isClosed = false

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        if (isClosed) return

        queue.add(block)
    }

    fun pump() {
        var remaining = queue.size

        while (remaining-- > 0) {
            val task = queue.poll() ?: break

            task.run()
        }
    }

    fun close() {
        queue.clear()
        isClosed = true
    }
}

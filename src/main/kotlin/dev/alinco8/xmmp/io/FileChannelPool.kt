package dev.alinco8.xmmp.io

import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FileChannelPool(
    private val maxOpen: Int,
) {
    private class Entry(val channel: FileChannel) {
        var refCount: Int = 0
    }

    private val mutex = Mutex()
    private val open = LinkedHashMap<Path, Entry>(16, 0.75f, true)

    suspend fun <T> useInIO(path: Path, create: Boolean, block: (FileChannel?) -> T): T {
        val entry = acquire(path, create) ?: return block(null)

        try {
            return withContext(Dispatchers.IO) { block(entry.channel) }
        } finally {
            withContext(NonCancellable) { release(entry) }
        }
    }

    private suspend fun acquire(path: Path, create: Boolean): Entry? = mutex.withLock {
        open[path]?.let {
            it.refCount++
            return@withLock it
        }

        if (!create && !path.exists()) return@withLock null

        val channel = withContext(Dispatchers.IO) {
            path.parent.createDirectories()
            FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
            )
        }

        Entry(channel).also {
            it.refCount++
            open[path] = it
        }
    }

    private suspend fun release(entry: Entry) = mutex.withLock {
        entry.refCount--
        evictIfNeeded()
    }

    private fun evictIfNeeded() {
        if (open.size <= maxOpen) return

        val it = open.entries.iterator()
        while (it.hasNext() && open.size > maxOpen) {
            val (_, e) = it.next()
            if (e.refCount > 0) continue
            runCatching { e.channel.close() }
            it.remove()
        }
    }

    suspend fun close() = mutex.withLock {
        open.values.forEach { runCatching { it.channel.close() } }
        open.clear()
    }
}

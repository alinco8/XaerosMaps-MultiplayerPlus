package dev.alinco8.xmmp.client.io

import dev.alinco8.xmmp.core.RegionKey
import dev.alinco8.xmmp.XMMP.LOGGER
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CursorStore private constructor(
    private val path: Path,
    private val cursors: ConcurrentHashMap<RegionKey, Long>,
) {
    @Volatile
    private var dirty = false

    companion object {
        const val MAGIC = 0x584D4343
        const val VERSION = 1

        const val HEADER_SIZE = Int.SIZE_BYTES + Int.SIZE_BYTES + Int.SIZE_BYTES
        const val ENTRY_SIZE = Int.SIZE_BYTES + Int.SIZE_BYTES + Long.SIZE_BYTES

        suspend fun open(dir: Path): CursorStore = withContext(Dispatchers.IO) {
            val path = dir.resolve("cursors.xmcc")
            val cursors = ConcurrentHashMap<RegionKey, Long>()

            runCatching {
                if (!path.exists()) return@runCatching

                val buf = ByteBuffer.wrap(path.readBytes())
                if (buf.remaining() < HEADER_SIZE) error("truncated header")

                if (buf.int != MAGIC) error("bad magic")
                if (buf.int != VERSION) error("bad version")

                val count = buf.int
                if (count < 0) error("bad count")
                if (buf.remaining() != count * ENTRY_SIZE) error("entry size mismatch")

                repeat(count) {
                    cursors[RegionKey(buf.int, buf.int)] = buf.long
                }
            }.onFailure {
                LOGGER.error("Failed to load cursors from $dir", it)
                cursors.clear()

                runCatching {
                    path.moveTo(path.resolveSibling("${path.fileName}.bak"), true)
                }
            }

            CursorStore(path, cursors)
        }
    }

    fun cursor(region: RegionKey) = cursors[region] ?: 0L
    fun setCursor(region: RegionKey, revision: Long) {
        cursors[region] = revision
        dirty = true
    }

    suspend fun flush() = withContext(Dispatchers.IO) {
        if (!dirty) return@withContext
        dirty = false

        try {
            val snapshot = cursors.toMap()
            val buf = ByteBuffer.allocate(HEADER_SIZE + snapshot.size * ENTRY_SIZE)

            buf.putInt(MAGIC)
            buf.putInt(VERSION)

            buf.putInt(snapshot.size)
            snapshot.forEach { (key, value) ->
                buf.putInt(key.x)
                buf.putInt(key.z)
                buf.putLong(value)
            }

            path.parent.createDirectories()
            val tmpFile = createTempFile(path.parent, path.fileName.toString(), ".tmp")
            tmpFile.writeBytes(buf.array())
            tmpFile.moveTo(
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: Exception) {
            LOGGER.error("Failed to flush cursors to $path", e)
            dirty = true
        }
    }
}

package dev.alinco8.xmmp.server.io

import com.github.luben.zstd.Zstd
import dev.alinco8.xmmp.MC_REGION_SIZE
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.io.FileChannelPool
import dev.alinco8.xmmp.io.TilePayloadCodec
import dev.alinco8.xmmp.io.PayloadHash
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.BitSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.math.max
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RegionFile(private val path: Path, private val pool: FileChannelPool) {
    private val ioMutex = Mutex()
    private var headerLoaded = false

    private val maxRevision = AtomicLong(0L)
    private val offset = IntArray(MC_REGION_SIZE * MC_REGION_SIZE)
    private val length = IntArray(MC_REGION_SIZE * MC_REGION_SIZE)
    private val revisions = AtomicLongArray(MC_REGION_SIZE * MC_REGION_SIZE)
    private val used = BitSet().apply { set(0, HEADER_SECTORS) }

    private val pending = ConcurrentHashMap<Int, Pending>()
    private val hashes = AtomicLongArray(MC_REGION_SIZE * MC_REGION_SIZE)

    private data class Pending(val payload: ByteArray, val revision: Long, val hash: Long) {
        override fun equals(other: Any?) =
            other is Pending && revision == other.revision
                    && hash == other.hash && payload.contentEquals(other.payload)

        override fun hashCode() = 31 * hash.hashCode() + revision.hashCode()
    }

    companion object {
        private fun sectors(size: Int) = (size + SECTOR_SIZE - 1) / SECTOR_SIZE

        const val MAGIC = 0x584D5352 // "XMSR"
        const val VERSION = 2

        const val HEADER_SIZE = Int.SIZE_BYTES + Int.SIZE_BYTES +
                MC_REGION_SIZE * MC_REGION_SIZE * (Int.SIZE_BYTES + Int.SIZE_BYTES + Long.SIZE_BYTES + Long.SIZE_BYTES)

        const val SECTOR_SIZE = 4096
        val HEADER_SECTORS = sectors(HEADER_SIZE)
    }

    /**
     * Caller must hold the mutex lock
     */
    private suspend fun ensureHeader() {
        if (headerLoaded) return

        pool.useInIO(path, false) { c ->
            runCatching {
                c?.let(::loadHeader)
            }.onFailure {
                LOGGER.error("Failed to load region file header from $path", it)
            }
        }
        headerLoaded = true
    }

    private fun loadHeader(c: FileChannel) {
        val size = c.size()
        val head = ByteBuffer.allocate(HEADER_SECTORS * SECTOR_SIZE)

        if (size == 0L) return
        if (size < head.capacity() || !c.readFully(head, 0L)) {
            LOGGER.warn(
                "Region file {} is too small to contain a valid header, reinitializing",
                path
            )

            return
        }
        head.flip()

        if (head.getInt() != MAGIC) {
            LOGGER.warn(
                "Region file {} has invalid magic number, reinitializing",
                path
            )
            return
        }
        if (head.getInt() != VERSION) {
            LOGGER.warn(
                "Region file {} has invalid version number, reinitializing",
                path
            )
            return
        }

        var headerMax = 0L
        for (i in 0 until MC_REGION_SIZE * MC_REGION_SIZE) {
            val offset = head.getInt()
            val len = head.getInt()
            val revision = head.getLong()
            val hash = head.getLong()

            this.offset[i] = offset
            this.length[i] = len
            this.revisions.set(i, revision)
            this.hashes.set(i, hash)

            if (offset != 0) used.set(offset, offset + sectors(len))

            if (revision > headerMax) headerMax = revision
        }

        this.maxRevision.updateAndGet { it.coerceAtLeast(headerMax) }
    }

    private fun index(localChunkX: Int, localChunkZ: Int) =
        localChunkX * MC_REGION_SIZE + localChunkZ

    suspend fun regionRevision() = ioMutex.withLock {
        ensureHeader()
        maxRevision.get().takeIf { it != 0L }
    }

    fun cachedRegionRevision() = maxRevision.get().takeIf { it != 0L }

    suspend fun chunkRevision(localChunkX: Int, localChunkZ: Int): Long? {
        val idx = index(localChunkX, localChunkZ)
        pending[idx]?.let { return it.revision }

        ioMutex.withLock {
            ensureHeader()
            return revisions.get(idx).takeIf { it != 0L }
        }
    }

    suspend fun readChunkWithRevision(localChunkX: Int, localChunkZ: Int): Pair<ByteArray, Long>? {
        val idx = index(localChunkX, localChunkZ)
        pending[idx]?.let { return it.payload to it.revision }

        return ioMutex.withLock {
            ensureHeader()

            val rev = revisions.get(idx).takeIf { it != 0L } ?: return@withLock null
            val payload = readFromDisk(idx) ?: return@withLock null

            payload to rev
        }
    }

    private suspend fun readFromDisk(idx: Int): ByteArray? {
        val off = offset[idx]
        if (off == 0) return null
        val len = length[idx]

        return pool.useInIO(path, false) { c ->
            if (c == null) return@useInIO null

            val buf = ByteBuffer.allocate(len)
            if (!c.readFully(buf, off.toLong() * SECTOR_SIZE)) return@useInIO null

            runCatching {
                val compressed = buf.array()
                val size = Zstd.getFrameContentSize(compressed)
                if (size !in 0 until Int.MAX_VALUE) return@useInIO null

                Zstd.decompress(compressed, size.toInt())
            }.getOrNull()
        }
    }

    suspend fun writeChunk(localChunkX: Int, localChunkZ: Int, payload: ByteArray): Long? {
        ioMutex.withLock { ensureHeader() }

        val idx = index(localChunkX, localChunkZ)
        val hash = PayloadHash.of(payload)
        var revision: Long? = null

        pending.compute(idx) { _, cur ->
            val storedHash = cur?.hash ?: hashes.get(idx)
            if (storedHash == hash) {
                cur
            } else {
                Pending(
                    payload,
                    maxRevision.updateAndGet { max(System.currentTimeMillis(), it + 1) },
                    hash
                ).also {
                    revision = it.revision
                }
            }
        }

        return revision
    }

    suspend fun flush() {
        if (pending.isEmpty()) return

        ioMutex.withLock {
            ensureHeader()

            val batch = pending.toMap()
            if (batch.isEmpty()) return

            pool.useInIO(path, true) { c -> commit(c!!, batch) }
            batch.forEach { (idx, p) -> pending.remove(idx, p) }
        }
    }

    private fun commit(c: FileChannel, batch: Map<Int, Pending>) {
        val toFree = ArrayList<Pair<Int, Int>>()

        for ((idx, p) in batch) {
            val compressed = Zstd.compress(p.payload, 3)
            val need = sectors(compressed.size)
            val newOff = allocate(need)

            c.writeFully(ByteBuffer.wrap(compressed), newOff.toLong() * SECTOR_SIZE)

            if (offset[idx] != 0) toFree.add(offset[idx] to sectors(length[idx]))
            offset[idx] = newOff
            length[idx] = compressed.size
            revisions.set(idx, p.revision)
            hashes.set(idx, p.hash)
        }

        c.force(false)
        writeHeader(c)
        c.force(false)

        for ((off, len) in toFree) used.clear(off, off + len)
    }

    private fun allocate(need: Int): Int {
        var start = HEADER_SECTORS
        while (true) {
            val s = used.nextClearBit(start)
            val e = used.nextSetBit(s)
            if (e == -1 || e - s >= need) {
                used.set(s, s + need)
                return s
            }
            start = e
        }
    }

    private fun writeHeader(c: FileChannel) {
        val buf = ByteBuffer.allocate(HEADER_SECTORS * SECTOR_SIZE)
        buf.putInt(MAGIC)
        buf.putInt(VERSION)

        for (i in 0 until MC_REGION_SIZE * MC_REGION_SIZE) {
            buf.putInt(offset[i])
            buf.putInt(length[i])
            buf.putLong(revisions.get(i))
            buf.putLong(hashes.get(i))
        }

        buf.clear()
        c.writeFully(buf, 0L)
    }
}

private fun FileChannel.readFully(buf: ByteBuffer, position: Long): Boolean {
    var pos = position
    while (buf.hasRemaining()) {
        val n = read(buf, pos)
        if (n < 0) return false
        pos += n
    }

    return true
}

private fun FileChannel.writeFully(buf: ByteBuffer, position: Long) {
    var pos = position
    while (buf.hasRemaining()) {
        pos += write(buf, pos)
    }
}

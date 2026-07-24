package dev.alinco8.xmmp.io

import com.github.luben.zstd.Zstd
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.file.Path
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class XMMPRegionFile(path: Path) : Closeable {
    private val lock = ReentrantReadWriteLock()
    private val raf = RandomAccessFile(path.toFile(), "rw")

    private val usedSectors = TreeMap<Int, Int>()

    companion object {
        const val REGION_SIZE = 32
        const val CHUNK_COUNT = REGION_SIZE * REGION_SIZE

        const val FILE_VERSION_START = 0
        const val REGION_TS_START = 4
        const val OFFSET_TABLE_START = REGION_TS_START + 8
        const val TS_TABLE_START = OFFSET_TABLE_START + CHUNK_COUNT * 4
        const val DATA_START = TS_TABLE_START + CHUNK_COUNT * 8

        const val CURRENT_VERSION = 2
        const val SECTOR_SIZE = 4096
    }

    init {
        if (raf.length() < DATA_START) {
            raf.setLength(DATA_START.toLong())
            raf.seek(FILE_VERSION_START.toLong())
            raf.writeInt(CURRENT_VERSION)
        } else {
            raf.seek(FILE_VERSION_START.toLong())

            when (val version = raf.readInt()) {
                1 -> {
                    raf.setLength(0)
                    raf.setLength(DATA_START.toLong())
                    raf.seek(FILE_VERSION_START.toLong())
                    raf.writeInt(CURRENT_VERSION)
                }

                2 -> {}
                else -> error("Unknown version $version")
            }
        }

        for (i in 0 until CHUNK_COUNT) {
            raf.seek((OFFSET_TABLE_START + i * 4).toLong())
            val packed = raf.readInt()
            if (packed == 0) continue

            val sector = packed ushr 8
            val count = packed and 0xFF

            usedSectors[sector] = count
        }
    }

    fun readChunk(chunkX: Int, chunkZ: Int): ByteArray? {
        lock.readLock().withLock {
            val idx = index(chunkX, chunkZ)
            raf.seek((OFFSET_TABLE_START + idx * 4).toLong())
            val packed = raf.readInt()
            if (packed == 0) return null

            val sector = packed ushr 8
            val count = packed and 0xFF
            raf.seek(DATA_START + sector.toLong() * SECTOR_SIZE)

            val size = raf.readInt()
            if (size !in 1..(count * SECTOR_SIZE - 4)) return null
            val compressed = ByteArray(size).also { raf.readFully(it) }
            val originalSize = Zstd.getFrameContentSize(compressed).toInt()

            return Zstd.decompress(compressed, originalSize)
        }
    }

    fun writeChunk(localChunkX: Int, localChunkZ: Int, data: ByteArray) {
        val compressed = Zstd.compress(data, 3)
        val sectorCount = (compressed.size + 4 + SECTOR_SIZE - 1) / SECTOR_SIZE

        require(sectorCount in 1..0xFF) {
            "Chunk ($localChunkX, $localChunkZ) too large: $sectorCount sectors (${compressed.size} bytes)"
        }

        lock.writeLock().withLock {
            val idx = index(localChunkX, localChunkZ)
            val sector = allocateSector(idx, sectorCount)

            raf.seek((OFFSET_TABLE_START + idx * 4).toLong())
            raf.writeInt((sector shl 8) or (sectorCount and 0xFF))

            raf.seek((TS_TABLE_START + idx * 8).toLong())
            raf.writeLong(System.currentTimeMillis())

            raf.seek(DATA_START + sector.toLong() * SECTOR_SIZE)
            raf.writeInt(compressed.size)
            raf.write(compressed)

            updateRegionTimestamp()
        }
    }

    fun getRegionTimestamp(): Long {
        lock.readLock().withLock {
            raf.seek(REGION_TS_START.toLong())
            return raf.readLong()
        }
    }

    private fun updateRegionTimestamp() {
        raf.seek(REGION_TS_START.toLong())
        raf.writeLong(System.currentTimeMillis())
    }

    private fun allocateSector(idx: Int, sectorCount: Int): Int {
        val existing = run {
            raf.seek((OFFSET_TABLE_START + idx * 4).toLong())
            val packed = raf.readInt()
            if (packed != 0) packed ushr 8 to (packed and 0xFF) else null
        }
        existing?.let { (oldSector, oldCount) ->
            if (sectorCount <= oldCount) {
                return oldSector
            }

            usedSectors.remove(oldSector)
        }

        var candidate = 0
        for ((sector, count) in usedSectors) {
            if (candidate + sectorCount <= sector) break
            candidate = sector + count
        }

        usedSectors[candidate] = sectorCount
        return candidate
    }

    private fun index(chunkX: Int, chunkZ: Int): Int {
        val lx = chunkX and 31
        val lz = chunkZ and 31
        return lx + lz * REGION_SIZE
    }

    override fun close() {
        lock.writeLock().withLock {
            raf.close()
        }
    }

}

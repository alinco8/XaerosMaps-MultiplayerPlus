package dev.alinco8.xmmp.io

import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class XMMPRegionCache(
    private val baseDir: Path,
    private val maxOpenFiles: Int = 64
) : Closeable {
    private val invalidFilenameRegex = Regex("[^a-zA-Z0-9_-]")
    private val dirtyData = ConcurrentHashMap<RegionKey, ConcurrentHashMap<ChunkKey, ByteArray>>()

    private val openFiles =
        object : LinkedHashMap<RegionKey, XMMPRegionFile>(maxOpenFiles, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<RegionKey, XMMPRegionFile>): Boolean {
                if (size > maxOpenFiles) {
                    eldest.value.close()
                    return true
                }
                return false
            }
        }
    private val filesLock = ReentrantReadWriteLock()

    fun writeChunk(dimensionId: String, chunkX: Int, chunkZ: Int, data: ByteArray) {
        val (regionKey, chunkKey) = toKeys(dimensionId, chunkX, chunkZ)

        dirtyData.getOrPut(regionKey) { ConcurrentHashMap() }[chunkKey] = data
    }

    fun readChunk(dimensionId: String, chunkX: Int, chunkZ: Int): ByteArray? {
        val (regionKey, chunkKey) = toKeys(dimensionId, chunkX, chunkZ)

        dirtyData[regionKey]?.get(chunkKey)?.let { return it }

        return filesLock.writeLock().withLock {
            getFileLocked(regionKey)?.readChunk(chunkKey.lx, chunkKey.lz)
        }
    }

    private fun getRegionTimestamp(regionKey: RegionKey): Long? =
        filesLock.writeLock().withLock {
            getFileLocked(regionKey)?.getRegionTimestamp()
        }

    fun getAllRegionTimestamps(dimensionId: String): Map<Long, Long> = buildMap {
        dimensionPath(dimensionId).toFile().listFiles { f -> f.extension == "xmmp" }
            ?.forEach { file ->
                val parts = file.nameWithoutExtension.removePrefix("r.").split(".")
                if (parts.size != 2) return@forEach
                val rx = parts[0].toIntOrNull() ?: return@forEach
                val rz = parts[1].toIntOrNull() ?: return@forEach

                val packed = rx.toLong() shl 32 or (rz.toLong() and 0xFFFFFFFFL)
                val ts = getRegionTimestamp(RegionKey(dimensionId, rx, rz)) ?: 0L
                put(packed, ts)
            }

        dirtyData.keys.filter { it.dimensionId == dimensionId }.forEach { rk ->
            val packed = rk.rx.toLong() shl 32 or (rk.rz.toLong() and 0xFFFFFFFFL)
            put(packed, System.currentTimeMillis())
        }
    }

    fun flush() {
        for ((regionKey, chunks) in dirtyData) {
            if (chunks.isEmpty()) continue

            filesLock.writeLock().withLock {
                val file = getOrCreateFileLocked(regionKey)

                for ((chunkKey, data) in chunks) {
                    file.writeChunk(
                        regionKey.rx * 32 + chunkKey.lx,
                        regionKey.rz * 32 + chunkKey.lz,
                        data
                    )
                    chunks.remove(chunkKey, data)
                }
            }

            dirtyData.computeIfPresent(regionKey) { _, m -> if (m.isEmpty()) null else m }
        }
    }

    override fun close() {
        flush()

        filesLock.writeLock().withLock {
            openFiles.values.forEach { it.close() }
            openFiles.clear()
        }
    }

    /**
     * Callers must hold filesLock.writeLock()
     */
    private fun getOrCreateFileLocked(regionKey: RegionKey): XMMPRegionFile =
        openFiles.getOrPut(regionKey) {
            val path = regionPath(regionKey)
            path.parent.createDirectories()

            XMMPRegionFile(path)
        }

    /**
     * Callers must hold filesLock.writeLock()
     */
    private fun getFileLocked(regionKey: RegionKey): XMMPRegionFile? {
        val path = regionPath(regionKey)
        if (!path.exists()) return null

        return openFiles.getOrPut(regionKey) { XMMPRegionFile(path) }
    }

    private fun dimensionPath(dimensionId: String) =
        baseDir.resolve(dimensionId.replace(invalidFilenameRegex, "_"))

    private fun regionPath(regionKey: RegionKey) = dimensionPath(regionKey.dimensionId)
        .resolve("r.${regionKey.rx}.${regionKey.rz}.xmmp")

    private fun toKeys(dimensionId: String, chunkX: Int, chunkZ: Int) = RegionKey(
        dimensionId,
        chunkX shr 5,
        chunkZ shr 5
    ) to ChunkKey(
        chunkX and 0x1F,
        chunkZ and 0x1F
    )

    data class RegionKey(val dimensionId: String, val rx: Int, val rz: Int)
    data class ChunkKey(val lx: Int, val lz: Int)
}

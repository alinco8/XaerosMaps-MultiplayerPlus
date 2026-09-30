package dev.alinco8.xmmp.server.io

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.RegionKey
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.io.FileChannelPool
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServerRegionStore(
    private val pool: FileChannelPool,
    private val directory: Path,
) {
    private val regions = ConcurrentHashMap<RegionKey, RegionFile>()

    companion object {
        const val FILE_EXT = "xmsr"
    }

    private fun regionReadonly(key: ChunkKey) = regionReadonly(key.toRegion())

    fun regionReadonly(key: RegionKey): RegionFile? {
        regions[key]?.let { return it }

        val path = directory.resolve("r.${key.x}.${key.z}.$FILE_EXT")
        if (!Files.exists(path)) {
            return null
        }

        return regions.computeIfAbsent(key) {
            RegionFile(path, pool)
        }
    }

    fun region(key: ChunkKey) =
        region(key.toRegion())

    private fun region(key: RegionKey) = regions.computeIfAbsent(key) {
        RegionFile(directory.resolve("r.${key.x}.${key.z}.$FILE_EXT"), pool)
    }

    private val fileNamePattern = Regex("""r\.(-?\d+)\.(-?\d+)\.$FILE_EXT""")

    suspend fun listRegionRevisions(): Map<RegionKey, Long> = withContext(Dispatchers.IO) {
        val paths =
            if (Files.exists(directory)) Files.list(directory).use { it.toList() }
            else emptyList()

        buildMap {
            for (path in paths) {
                val match = fileNamePattern.matchEntire(path.name) ?: continue
                val key = RegionKey(
                    match.groupValues[1].toIntOrNull() ?: continue,
                    match.groupValues[2].toIntOrNull() ?: continue,
                )

                val revision = try {
                    regionReadonly(key)?.regionRevision()
                } catch (e: IOException) {
                    LOGGER.warn("Failed to read region revision for $path, skipping", e)
                    continue
                }

                if (revision != null) put(key, revision)
            }

            for ((key, region) in regions) {
                if (key in this) continue
                region.cachedRegionRevision()?.let { put(key, it) }
            }
        }
    }

    suspend fun chunkRevision(pos: ChunkKey) = regionReadonly(pos)
        ?.chunkRevision(pos.localX(), pos.localZ())

    suspend fun readChunkWithRevision(pos: ChunkKey) = regionReadonly(pos)
        ?.readChunkWithRevision(pos.localX(), pos.localZ())

    suspend fun writeChunk(pos: ChunkKey, payload: ByteArray) = region(pos)
        .writeChunk(pos.localX(), pos.localZ(), payload)

    suspend fun flush() = regions.values.forEach { it.flush() }
}

package dev.alinco8.xmmp.client.xaero

import dev.alinco8.xmmp.ChunkKey
import dev.alinco8.xmmp.XAERO_TILE_CHUNK_SIZE
import dev.alinco8.xmmp.XAERO_TILE_SIZE
import dev.alinco8.xmmp.io.TilePayloadCodec
import dev.alinco8.xmmp.mixin.compat.xaeroworldmap.MapPixelAccessor
import kotlinx.coroutines.yield
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import xaero.map.MapProcessor
import xaero.map.region.MapBlock
import xaero.map.region.MapRegion
import xaero.map.region.MapTile
import xaero.map.region.MapTileChunk
import xaero.map.region.OverlayBuilder

internal suspend fun <T> awaitResult(
    maxAttempts: Int = 200,
    block: () -> XaeroController.Result<T>,
): T? {
    repeat(maxAttempts) {
        when (val result = block()) {
            is XaeroController.Result.Success -> return result.value
            XaeroController.Result.NotFound -> return null
            XaeroController.Result.Loading -> yield()
        }
    }

    return null
}

object XaeroController {
    private const val SURFACE_CAVE_LAYER = Int.MAX_VALUE
    private const val NO_CAVE_START = Int.MAX_VALUE

    sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        object Loading : Result<Nothing>()
        object NotFound : Result<Nothing>()

        fun <R> map(transform: (T) -> R): Result<R> = when (this) {
            is Success -> Success(transform(value))
            Loading -> Loading
            NotFound -> NotFound
        }
    }

    enum class RegionState(val value: Byte) {
        UNPROCESSED(0),
        LOADING(1),
        LOADED(2),
        UPLOADING(3),
        FINISHED(4);

        companion object {
            fun fromByte(value: Byte) = entries.find { it.value == value }
        }
    }

    private val MapTileChunk.isAwaitingTextureReadback get() = leafTexture.shouldDownloadFromPBO()

    private fun MapTile.readBlocksOrNull(): Array<TilePayloadCodec.BlockData>? {
        val blocks = arrayOfNulls<TilePayloadCodec.BlockData>(XAERO_TILE_SIZE * XAERO_TILE_SIZE)

        for (index in blocks.indices) {
            val x = index / XAERO_TILE_SIZE
            val z = index % XAERO_TILE_SIZE

            val block = getBlock(x, z) ?: return null
            val state = block.state ?: return null

            blocks[index] = TilePayloadCodec.BlockData(
                state,
                block.height.toShort(),
                block.topHeight.toShort(),
                block.biome,
                (block as MapPixelAccessor).light,
                (block as MapPixelAccessor).glowing,
                block.overlays?.mapNotNull { overlay ->
                    val overlayState = overlay.state ?: return@mapNotNull null

                    TilePayloadCodec.BlockOverlay(
                        overlayState,
                        (overlay as MapPixelAccessor).light,
                        (overlay as MapPixelAccessor).glowing,
                        overlay.opacity.toShort(),
                    )
                } ?: emptyList(),
            )
        }

        @Suppress("UNCHECKED_CAST")
        return blocks as Array<TilePayloadCodec.BlockData>
    }

    private fun requestLoad(region: MapRegion, processor: MapProcessor) {
        synchronized(region) {
            if (region.canRequestReload_unsynced()) {
                region.isBeingWritten = true

                processor.mapSaveLoad.requestLoad(
                    region,
                    "xmmp@${region.caveLayer}_${region.regionX}_${region.regionZ}"
                )
            }
        }
    }

    fun MapProcessor.writeTile(
        dimension: ResourceKey<Level>,
        key: ChunkKey,
        tileData: TilePayloadCodec.TileData,
        blocks: Array<TilePayloadCodec.BlockData>,
    ): Result<Unit> {
        synchronized(renderThreadPauseSync) {
            if (!isWritable()) return Result.Loading
            if (mapWorld.currentDimensionId != dimension) return Result.Loading

            val regionKey = key.toRegion()
            val region = getLeafMapRegion(
                SURFACE_CAVE_LAYER,
                regionKey.x,
                regionKey.z,
                true
            ) ?: return Result.NotFound

            synchronized(region.writerThreadPauseSync) {
                if (region.isWritingPaused) return Result.Loading

                val tileChunkLocalX = key.tileChunkX()
                val tileChunkLocalZ = key.tileChunkZ()
                val tileChunk: MapTileChunk
                val tileChunkWasCreated: Boolean

                synchronized(region) {
                    if (!region.prepareForWrite_locked(this)) return Result.Loading

                    val existing = region.getChunk(tileChunkLocalX, tileChunkLocalZ)
                    if (existing != null && !existing.hasData) return Result.Loading

                    tileChunkWasCreated = existing == null
                    tileChunk = existing ?: MapTileChunk(
                        region,
                        Math.floorDiv(key.globalX, XAERO_TILE_CHUNK_SIZE),
                        Math.floorDiv(key.globalZ, XAERO_TILE_CHUNK_SIZE),
                    ).also {
                        it.setLoadState(RegionState.LOADED.value)
                        region.setChunk(tileChunkLocalX, tileChunkLocalZ, it)
                        region.isAllCachePrepared = false
                    }
                }

                if (tileChunk.isAwaitingTextureReadback) return Result.Loading

                val tileX = key.tileX()
                val tileZ = key.tileZ()

                val tile = tileChunk.getTile(
                    tileX,
                    tileZ,
                ) ?: this.tilePool.get(
                    this.currentDimension,
                    key.globalX,
                    key.globalZ,
                )

                tile.applyBlocks(
                    this,
                    blocks,
                )

                tile.worldInterpretationVersion = tileData.worldInterpretationVersion

                tile.isLoaded = true
                tile.setWrittenOnce(true)
                tile.setWrittenCave(NO_CAVE_START, this.caveModeDepthConfig)

                tileChunk.setTile(
                    tileX,
                    tileZ,
                    tile,
                    this.blockStateShortShapeCache,
                    this,
                )

                tileChunk.setChanged(true)
                tileChunk.toUpdateBuffers = true

                fun markNeighbour(dx: Int, dz: Int) {
                    val nx = tileChunkLocalX + dx
                    val nz = tileChunkLocalZ + dz
                    if (nx > 7 || nz > 7) return
                    region.getChunk(nx, nz)?.takeIf { it.hasData }?.toUpdateBuffers = true
                }

                val lastTile = XAERO_TILE_CHUNK_SIZE - 1
                if (tileX == lastTile) markNeighbour(1, 0)
                if (tileZ == lastTile) markNeighbour(0, 1)
                if (tileX == lastTile && tileZ == lastTile) markNeighbour(1, 1)

                tileChunk.setHasHadTerrain()

                region.setHasHadTerrain()

                if (tileChunkWasCreated) {
                    this.mapRegionHighlightsPreparer.prepare(
                        region,
                        tileChunkLocalX,
                        tileChunkLocalZ,
                        false,
                    )
                }

                invalidateOtherCaveLayers(region, this)

                return Result.Success(Unit)
            }
        }
    }

    private fun invalidateOtherCaveLayers(region: MapRegion, processor: MapProcessor) {
        if (region.isNormalMapData) return

        region.dim.layeredMapRegions.applyToEachLoadedLayer { caveLayer, _ ->
            if (caveLayer != region.caveLayer) {
                processor.getLeafMapRegion(
                    caveLayer,
                    region.regionX,
                    region.regionZ,
                    true,
                )?.also {
                    it.isOutdatedWithOtherLayers = true
                    it.setHasHadTerrain()
                }
            }
        }
    }

    private fun MapTile.applyBlocks(
        processor: MapProcessor,
        blocks: Array<TilePayloadCodec.BlockData>,
    ) {
        val overlayBuilder =
            OverlayBuilder(processor.overlayManager)

        for (x in 0 until XAERO_TILE_SIZE) {
            for (z in 0 until XAERO_TILE_SIZE) {
                val data = blocks[x * XAERO_TILE_SIZE + z]

                val block =
                    getBlock(x, z)
                        ?: MapBlock().also {
                            setBlock(x, z, it)
                        }

                block.prepareForWriting(0)

                block.write(
                    data.blockState,
                    data.height.toInt(),
                    data.topHeight.toInt(),
                    data.biome,
                    data.lightLevel,
                    data.glowing,
                    false,
                )

                overlayBuilder.startBuilding()

                for ((state, light, _, opacity) in data.overlays) {
                    overlayBuilder.build(
                        state,
                        opacity.toInt(),
                        light,
                        processor,
                        null,
                    )
                }

                overlayBuilder.finishBuilding(block)

                block.setSlopeUnknown(true)
            }
        }
    }

    private fun MapProcessor.isWritable() = !isWritingPaused
            && !isWaitingForWorldUpdate
            && mapSaveLoad.isRegionDetectionComplete
            && isCurrentMultiworldWritable

    private fun MapRegion.prepareForWrite_locked(processor: MapProcessor): Boolean {
        val state = RegionState.fromByte(loadState)

        if (state == RegionState.LOADED) registerVisit()
        if (!isResting) return false

        isBeingWritten = true

        return when (state) {
            RegionState.LOADED -> true

            RegionState.UNPROCESSED, RegionState.FINISHED -> {
                requestLoad(this, processor)

                false
            }

            else -> false
        }
    }

    private val MapTileChunk.hasData get() = loadState.let { it == 2 || it == 3 }

    fun MapTile.snapshot(): TileSnapshot? {
        return TileSnapshot(
            TilePayloadCodec.TileData(
                worldInterpretationVersion
            ),
            readBlocksOrNull() ?: return null
        )
    }
}

data class TileSnapshot(
    val tileData: TilePayloadCodec.TileData,
    val blocks: Array<TilePayloadCodec.BlockData>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TileSnapshot

        if (tileData != other.tileData) return false
        if (!blocks.contentEquals(other.blocks)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tileData.hashCode()
        result = 31 * result + blocks.contentHashCode()
        return result
    }
}

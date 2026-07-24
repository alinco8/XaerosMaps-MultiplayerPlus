package dev.alinco8.xmmp

import dev.alinco8.xmmp.XMMP.LOGGER
import dev.alinco8.xmmp.mixin.client.MapPixelAccessor
import dev.alinco8.xmmp.packet.ChunkDataPacket
import net.minecraft.client.Minecraft
import xaero.map.MapProcessor
import xaero.map.core.XaeroWorldMapCore
import xaero.map.region.MapBlock
import xaero.map.region.MapRegion
import xaero.map.region.MapTile
import xaero.map.region.MapTileChunk
import xaero.map.region.OverlayBuilder

object XaeroMapUtils {
    enum class LoadState(val value: Byte) {
        UNPROCESSED(0),
        LOADING(1),
        LOADED(2),
        UPLOADING(3),
        FINISHED(4);

        companion object {
            fun fromByte(value: Byte): LoadState? {
                return entries.find { it.value == value }
            }
        }
    }

    /**
     * Do NOT call this for regions that are not LOADED
     */
    fun createChunkDataPacket(chunkX: Int, chunkZ: Int): ChunkDataPacket? {
        val level = Minecraft.getInstance().level ?: run {
            LOGGER.warn("No level for chunk packet: {}, {}", chunkX, chunkZ)
            return null
        }
        val session = XaeroWorldMapCore.currentSession ?: run {
            LOGGER.warn("No session for chunk packet: {}, {}", chunkX, chunkZ)
            return null
        }
        val processor = session.mapProcessor ?: run {
            LOGGER.warn("No processor for chunk packet: {}, {}", chunkX, chunkZ)
            return null
        }

        val region = processor.getLeafMapRegion(
            Int.MAX_VALUE,
            chunkX.chunkToRegion(),
            chunkZ.chunkToRegion(),
            false
        ) ?: run {
            //TODO: create option is false, so if the region is not found,
            //  it may be better to stop rather than retry
            LOGGER.warn("No region for chunk packet: {}, {}", chunkX, chunkZ)
            return null
        }
        if (region.loadState != LoadState.LOADED.value) {
            LOGGER.warn(
                "Region not loaded for chunk packet: {}, {}, loadState: {}",
                chunkX,
                chunkZ,
                region.loadState
            )
            return null
        }

        val tileChunk =
            region.getChunk(chunkX.chunkToTileChunk(), chunkZ.chunkToTileChunk()) ?: run {
                LOGGER.warn("No tile chunk for chunk packet: {}, {}", chunkX, chunkZ)
                return null
            }
        val tile = tileChunk.getTile(chunkX.chunkToTile(), chunkZ.chunkToTile()) ?: run {
            LOGGER.warn("No tile for chunk packet: {}, {}", chunkX, chunkZ)
            return null
        }

        return ChunkDataPacket(
            chunkX,
            chunkZ,
            ChunkDataPacket.TileData(
                tile.worldInterpretationVersion
            ),
            Array(TILE_SIZE * TILE_SIZE) { index ->
                val x = index / TILE_SIZE
                val z = index % TILE_SIZE

                tile.getBlock(x, z).run {
                    ChunkDataPacket.BlockData(
                        state,
                        height.toShort(),
                        topHeight.toShort(),
                        biome,
                        (this as MapPixelAccessor).light,
                        (this as MapPixelAccessor).glowing,
                        overlays?.map { overlay ->
                            ChunkDataPacket.BlockOverlay(
                                overlay.state,
                                (overlay as MapPixelAccessor).light,
                                (overlay as MapPixelAccessor).glowing,
                                overlay.opacity.toShort()
                            )
                        } ?: emptyList()
                    )
                }
            },
            level.dimension().id().toString()
        )
    }

    fun getRegionTimestamp(rx: Int, rz: Int): Long? {
        val session = XaeroWorldMapCore.currentSession ?: return null
        val processor = session.mapProcessor ?: return null
        val region = processor.getLeafMapRegion(
            Int.MAX_VALUE,
            rx,
            rz,
            false
        ) ?: return null

        return region.lastSaveTime
    }

    fun applyChunkData(
        chunkX: Int,
        chunkZ: Int,
        tileData: ChunkDataPacket.TileData,
        blocks: Array<ChunkDataPacket.BlockData>
    ): Boolean {
        val session = XaeroWorldMapCore.currentSession ?: run {
            LOGGER.warn("No session for apply(), chunk: {}, {}", chunkX, chunkZ)
            return false
        }
        val processor = session.mapProcessor ?: run {
            LOGGER.warn("No processor for apply(), chunk: {}, {}", chunkX, chunkZ)
            return false
        }

        val region = processor.getLeafMapRegion(
            Int.MAX_VALUE,
            chunkX.chunkToRegion(),
            chunkZ.chunkToRegion(),
            true
        ) ?: run {
            LOGGER.warn("No region for apply(), chunk: {}, {}", chunkX, chunkZ)
            return false
        }

        when (val loadState = LoadState.fromByte(region.loadState)) {
            LoadState.UNPROCESSED -> {
                LOGGER.debug("region unprocessed, requesting load (chunk: {}, {})", chunkX, chunkZ)

                requestProperLoad(region, processor)
                return false
            }

            LoadState.LOADING -> {
                //TODO: retrying too many times may cause it to not load?

                LOGGER.debug(
                    "region loading, waiting for load to finish (chunk: {}, {})",
                    chunkX,
                    chunkZ
                )
                return false
            }

            LoadState.LOADED -> {}

            LoadState.UPLOADING -> {
                if (processor.mapSaveLoad.saveExists(region)) {
                    processor.mapSaveLoad.requestLoad(region, "xmmp@applyChunkData")

                    LOGGER.debug(
                        "region finished but save exists, requesting load (chunk: {}, {})",
                        chunkX,
                        chunkZ
                    )
                    return false
                }

                region.restoreBufferUpdateObjects()
                synchronized(region) {
                    region.loadState = LoadState.LOADED.value
                }
            }

            LoadState.FINISHED -> {
                LOGGER.debug(
                    "region finished, requesting load (chunk: {}, {})",
                    chunkX,
                    chunkZ
                )

                requestProperLoad(region, processor)

                return false
            }

            else -> {
                LOGGER.error("unexpected load state: {}", loadState)

                return false
            }
        }

        synchronized(region.writerThreadPauseSync) {
            if (region.isWritingPaused) {
                LOGGER.debug("region writing paused, retrying (chunk: {}, {})", chunkX, chunkZ)
                return false
            }

            return synchronized(region) {
                if (region.loadState != LoadState.LOADED.value || !region.isResting) {
                    LOGGER.debug("region not resting, retrying (chunk: {}, {})", chunkX, chunkZ)
                    return@synchronized false
                }

                region.isBeingWritten = true

                val tileChunkLocalX = chunkX.chunkToTileChunk()
                val tileChunkLocalZ = chunkZ.chunkToTileChunk()
                val tileChunkAbsX = Math.floorDiv(chunkX, TILE_CHUNK_SIZE)
                val tileChunkAbsZ = Math.floorDiv(chunkZ, TILE_CHUNK_SIZE)

                val tileChunk = region.getChunk(tileChunkLocalX, tileChunkLocalZ) ?: MapTileChunk(
                    region,
                    tileChunkAbsX,
                    tileChunkAbsZ
                ).also {
                    it.setLoadState(LoadState.LOADED.value)
                    region.setChunk(tileChunkLocalX, tileChunkLocalZ, it)
                    region.isAllCachePrepared = false
                }
                LOGGER.debug("tileChunk x:{}, z:{}", tileChunk.x, tileChunk.z)

                val tileX = chunkX.chunkToTile()
                val tileZ = chunkZ.chunkToTile()

                val tileAbsX = Math.floorDiv(chunkX, TILE_CHUNK_SIZE) * TILE_CHUNK_SIZE + tileX
                val tileAbsZ = Math.floorDiv(chunkZ, TILE_CHUNK_SIZE) * TILE_CHUNK_SIZE + tileZ

                val tile = tileChunk.getTile(tileX, tileZ) ?: processor.tilePool.get(
                    processor.currentDimension,
                    tileAbsX,
                    tileAbsZ
                )
                LOGGER.debug("tile x:{}, z:{}", tileX, tileZ)

                applyTileData(tile, blocks, processor)

                tile.worldInterpretationVersion = tileData.worldInterpretationVersion

                tile.isLoaded = true
                tile.setWrittenOnce(true)
                tile.setWrittenCave(Int.MAX_VALUE, 0)

                tileChunk.setTile(
                    tileX,
                    tileZ,
                    tile,
                    processor.blockStateShortShapeCache,
                    processor
                )

                tileChunk.setChanged(true)
                tileChunk.toUpdateBuffers = true
                tileChunk.setHasHadTerrain()

                region.setHasHadTerrain()

                true
            }
        }
    }

    private fun requestProperLoad(region: MapRegion, processor: MapProcessor) {
        synchronized(region) {
            if (region.canRequestReload_unsynced() && region.loadState != LoadState.LOADED.value) {
                region.isBeingWritten = true
                processor.mapSaveLoad.requestLoad(region, "xmmp@applyChunkData")
            }
        }
    }

    fun applyTileData(
        tile: MapTile,
        blocks: Array<ChunkDataPacket.BlockData>,
        processor: MapProcessor
    ) {
        val overlayManager = processor.overlayManager
        val overlayBuilder = OverlayBuilder(overlayManager)

        for (blockX in 0 until TILE_SIZE) {
            for (blockZ in 0 until TILE_SIZE) {
                val blockData = blocks[blockX * TILE_SIZE + blockZ]

                val block = tile.getBlock(blockX, blockZ) ?: MapBlock().also {
                    tile.setBlock(blockX, blockZ, it)
                }

                block.prepareForWriting(0)
                block.write(
                    blockData.blockState,
                    blockData.height.toInt(),
                    blockData.topHeight.toInt(),
                    blockData.biome,
                    blockData.lightLevel,
                    blockData.glowing,
                    false
                )

                overlayBuilder.startBuilding()
                for (overlay in blockData.overlays) {
                    overlayBuilder.build(
                        overlay.state,
                        overlay.opacity.toInt(),
                        overlay.light,
                        processor,
                        null
                    )
                }
                overlayBuilder.finishBuilding(block)

                block.setSlopeUnknown(true)
            }
        }
    }
}

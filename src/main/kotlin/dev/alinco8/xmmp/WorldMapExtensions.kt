package dev.alinco8.xmmp

const val REGION_SIZE = 8
const val TILE_CHUNK_SIZE = 4
const val TILE_SIZE = 16

fun Int.chunkToRegion() = Math.floorDiv(this, REGION_SIZE * TILE_CHUNK_SIZE)
fun Int.chunkToTileChunk() = Math.floorMod(Math.floorDiv(this, TILE_CHUNK_SIZE), REGION_SIZE)
fun Int.chunkToTile() = Math.floorMod(this, TILE_CHUNK_SIZE)

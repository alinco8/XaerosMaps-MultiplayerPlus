package dev.alinco8.xmmp

import dev.alinco8.xmmp.network.XMMPStreamCodec
import net.minecraft.world.level.ChunkPos

const val MC_REGION_SIZE = 32
val MC_REGION_SIZE_BITS = MC_REGION_SIZE.countTrailingZeroBits()

const val MC_CHUNK_SIZE = 16

const val XAERO_REGION_SIZE = 8
const val XAERO_TILE_CHUNK_SIZE = 4
const val XAERO_TILE_SIZE = 16

data class ChunkKey(val globalX: Int, val globalZ: Int) {
    companion object {
        val codec = with(XMMPStreamCodec) {
            composite(
                int, ChunkKey::globalX,
                int, ChunkKey::globalZ,
                ::ChunkKey
            )
        }

        fun fromLocal(region: RegionKey, localX: Int, localZ: Int) = ChunkKey(
            (region.x shl MC_REGION_SIZE_BITS) + localX,
            (region.z shl MC_REGION_SIZE_BITS) + localZ
        )

        fun toTileChunk(n: Int) = Math.floorMod(
            Math.floorDiv(n, XAERO_TILE_CHUNK_SIZE),
            XAERO_REGION_SIZE
        )

        fun toTile(n: Int) = Math.floorMod(n, XAERO_TILE_CHUNK_SIZE)
    }

    fun localIndex() = localX() * MC_REGION_SIZE + localZ()

    fun localX() = Math.floorMod(globalX, MC_REGION_SIZE)
    fun localZ() = Math.floorMod(globalZ, MC_REGION_SIZE)

    fun toRegion() = RegionKey(globalX shr MC_REGION_SIZE_BITS, globalZ shr MC_REGION_SIZE_BITS)
    fun toChunkPos() = ChunkPos(globalX, globalZ)

    fun tileChunkX() = toTileChunk(globalX)
    fun tileChunkZ() = toTileChunk(globalZ)

    fun tileX() = toTile(globalX)
    fun tileZ() = toTile(globalZ)
}

data class RegionKey(val x: Int, val z: Int) {
    companion object {
        val codec = with(XMMPStreamCodec) {
            composite(
                int, RegionKey::x,
                int, RegionKey::z,
                ::RegionKey
            )
        }
    }
}

package dev.alinco8.xmmp.core.common

import dev.alinco8.xmmp.core.ResourceId
import dev.alinco8.xmmp.core.network.XMMPPacket
import java.util.UUID

interface ServerHost {
    fun <T : XMMPPacket<T>> sendToPlayer(playerId: UUID, packet: T): Boolean
    suspend fun validateChunkBytes(chunkBytes: ByteArray): Boolean

    fun listPlayers(): List<UUID>

    fun getDimension(playerId: UUID): ResourceId?
    fun disconnectPlayer(playerId: UUID, reason: String)

    fun onDimensionChanged(playerId: UUID, dimension: ResourceId) {}
}

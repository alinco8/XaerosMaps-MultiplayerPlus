package dev.alinco8.xmmp.sync

//TODO: optimize this
class ChunkPosQueue<T> {
    companion object {
        const val ONE_SECOND_NANOS = 1_000_000_000L
        const val COOLDOWN_NANOS = 10L * ONE_SECOND_NANOS
    }

    data class Entry<T>(val x: Int, val z: Int, var data: T) {
        var retries = 0
    }

    private val lock = Any()
    private val entries = LinkedHashMap<Long, Entry<T>>()
    private val cooldowns = LinkedHashMap<Long, Long>()

    val size: Int get() = synchronized(lock) { entries.size }

    fun add(x: Int, z: Int, data: T, ignoreCooldown: Boolean = false) =
        add(Entry(x, z, data), ignoreCooldown)

    fun add(entry: Entry<T>, ignoreCooldown: Boolean = false) = synchronized(lock) {
        val packed = entry.x.toLong() and 4294967295L or ((entry.z.toLong() and 4294967295L) shl 32)
        if (!ignoreCooldown && cooldowns.getOrDefault(packed, 0L) > System.nanoTime()) {
            return@synchronized
        }

        val existing = entries[packed]
        if (existing != null) {
            existing.data = entry.data
        } else {
            entries[packed] = entry
        }
    }

    fun sortByDistance(playerX: Int, playerZ: Int) = synchronized(lock) {
        val sorted = entries.entries.sortedBy { (_, entry) ->
            val dx = (entry.x - playerX).toLong()
            val dz = (entry.z - playerZ).toLong()
            dx * dx + dz * dz
        }
        entries.clear()
        sorted.forEach { (key, entry) -> entries[key] = entry }
    }

    fun poll(): Entry<T>? = synchronized(lock) {
        val (key, entry) = entries.entries.firstOrNull() ?: return@synchronized null

        entries.remove(key)
        cooldowns[key] = System.nanoTime() + COOLDOWN_NANOS

        entry
    }

    fun clear() = synchronized(lock) {
        entries.clear()
        cooldowns.clear()
    }

    fun purgeCooldowns() = synchronized(lock) {
        val now = System.nanoTime()
        cooldowns.entries.removeIf { (_, expiry) -> expiry <= now }
    }
}

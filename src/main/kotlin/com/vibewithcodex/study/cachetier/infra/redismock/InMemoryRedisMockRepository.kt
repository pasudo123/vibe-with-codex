package com.vibewithcodex.study.cachetier.infra.redismock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.springframework.stereotype.Repository

/**
 * L2 Redis를 흉내 내는 shared in-memory repository.
 */
@Repository
class InMemoryRedisMockRepository : RedisMockRepository {

    private val store = ConcurrentHashMap<String, RedisMockEntry>()
    private val accessCounts = ConcurrentHashMap<String, AtomicLong>()

    override fun get(key: String): RedisLookupResult? {
        accessCounts.computeIfAbsent(key) { AtomicLong() }.incrementAndGet()

        val entry = store[key] ?: return null
        val now = System.currentTimeMillis()
        if (entry.expiresAtMillis <= now) {
            store.remove(key, entry)
            return null
        }

        return RedisLookupResult(
            value = entry.value,
            version = entry.version,
            ttlRemainingSeconds = ttlRemainingSeconds(entry.expiresAtMillis, now),
        )
    }

    override fun put(key: String, value: String, version: Long, ttlSeconds: Long) {
        require(ttlSeconds > 0) { "ttlSeconds must be greater than 0" }
        store[key] = RedisMockEntry(
            value = value,
            version = version,
            expiresAtMillis = System.currentTimeMillis() + ttlSeconds * 1000,
        )
    }

    override fun invalidate(key: String) {
        store.remove(key)
    }

    override fun clear() {
        store.clear()
        accessCounts.clear()
    }

    override fun getAccessCount(key: String): Long {
        return accessCounts[key]?.get() ?: 0
    }

    private fun ttlRemainingSeconds(expiresAtMillis: Long, nowMillis: Long): Long {
        val remainingMillis = expiresAtMillis - nowMillis
        return if (remainingMillis <= 0) 0 else (remainingMillis + 999) / 1000
    }
}

data class RedisMockEntry(
    val value: String,
    val version: Long,
    val expiresAtMillis: Long,
)

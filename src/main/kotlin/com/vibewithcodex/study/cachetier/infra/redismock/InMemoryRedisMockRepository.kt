package com.vibewithcodex.study.cachetier.infra.redismock

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

@Repository
class InMemoryRedisMockRepository : RedisMockRepository {

    private val store = ConcurrentHashMap<String, RedisMockEntry>()

    override fun get(key: String): String? {
        val entry = store[key] ?: return null
        val now = System.currentTimeMillis()
        if (entry.expiresAtMillis <= now) {
            store.remove(key)
            return null
        }
        return entry.value
    }

    override fun put(key: String, value: String, ttlSeconds: Long) {
        require(ttlSeconds > 0) { "ttlSeconds must be greater than 0" }
        val expiresAtMillis = System.currentTimeMillis() + (ttlSeconds * 1000)
        store[key] = RedisMockEntry(value, expiresAtMillis)
    }

    override fun clear() {
        store.clear()
    }
}

data class RedisMockEntry(
    val value: String,
    val expiresAtMillis: Long,
)

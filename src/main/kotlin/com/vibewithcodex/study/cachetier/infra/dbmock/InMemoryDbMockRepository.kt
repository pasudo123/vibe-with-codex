package com.vibewithcodex.study.cachetier.infra.dbmock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.springframework.stereotype.Repository

/**
 * Origin DB를 흉내 내는 in-memory repository.
 *
 * version은 쓰기마다 증가해 다중 pod local cache의 stale 여부를 눈으로 확인하게 해준다.
 */
@Repository
class InMemoryDbMockRepository : DbMockRepository {

    private val store = ConcurrentHashMap<String, DbMockEntry>()
    private val accessCounts = ConcurrentHashMap<String, AtomicLong>()

    override fun get(key: String): DbLookupResult? {
        accessCounts.computeIfAbsent(key) { AtomicLong() }.incrementAndGet()
        return store[key]?.let { DbLookupResult(value = it.value, version = it.version) }
    }

    override fun upsert(key: String, value: String): DbLookupResult {
        val updated = store.compute(key) { _, current ->
            DbMockEntry(
                value = value,
                version = (current?.version ?: 0) + 1,
            )
        } ?: error("db upsert failed: $key")
        return DbLookupResult(value = updated.value, version = updated.version)
    }

    override fun clear() {
        store.clear()
        accessCounts.clear()
    }

    override fun getAccessCount(key: String): Long {
        return accessCounts[key]?.get() ?: 0
    }
}

data class DbMockEntry(
    val value: String,
    val version: Long,
)

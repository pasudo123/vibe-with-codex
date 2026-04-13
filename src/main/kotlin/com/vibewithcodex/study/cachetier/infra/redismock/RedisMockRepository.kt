package com.vibewithcodex.study.cachetier.infra.redismock

interface RedisMockRepository {
    fun get(key: String): RedisLookupResult?
    fun put(key: String, value: String, version: Long, ttlSeconds: Long)
    fun invalidate(key: String)
    fun clear()
    fun getAccessCount(key: String): Long
}

data class RedisLookupResult(
    val value: String,
    val version: Long,
    val ttlRemainingSeconds: Long,
)

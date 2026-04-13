package com.vibewithcodex.study.cachetier.application

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.vibewithcodex.study.cachetier.config.StudyCacheTierProperties
import com.vibewithcodex.study.cachetier.domain.CacheLayer
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import com.vibewithcodex.study.cachetier.domain.CacheScenario
import com.vibewithcodex.study.cachetier.domain.CacheStatsResponse
import com.vibewithcodex.study.cachetier.domain.CacheTraceResult
import com.vibewithcodex.study.cachetier.domain.CacheTraceStep
import com.vibewithcodex.study.cachetier.infra.dbmock.DbMockRepository
import com.vibewithcodex.study.cachetier.infra.redismock.RedisMockRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import org.springframework.stereotype.Service

/**
 * 실무형 cache layer 흐름을 시뮬레이션하는 서비스.
 *
 * - L1: pod별 Caffeine local cache
 * - L2: 모든 pod가 공유하는 Redis mock
 * - Origin: DB mock
 */
@Service
class StudyCacheTierService(
    private val redisMockRepository: RedisMockRepository,
    private val dbMockRepository: DbMockRepository,
    private val properties: StudyCacheTierProperties,
) {
    private val localCaches = ConcurrentHashMap<String, Cache<String, LocalCacheEntry>>()
    private val keyLocks = ConcurrentHashMap<String, Any>()
    private val redisOnlyVersions = ConcurrentHashMap<String, Long>()

    init {
        repeat(properties.podCount) { index ->
            localCaches[podName(index + 1)] = newLocalCache()
        }
    }

    fun getData(scenario: CacheScenario, podId: String, key: String): CacheLookupResponse {
        val trace = mutableListOf<CacheTraceStep>()
        val localCache = localCache(podId)

        readLocal(localCache, key, trace)?.let {
            return hitResponse(key, it.value, it.version, CacheLayer.LOCAL, scenario, podId, it.ttlRemainingSeconds, trace)
        }

        val lockKey = "$podId:$key"
        val lock = keyLocks.computeIfAbsent(lockKey) { Any() }
        try {
            synchronized(lock) {
                readLocal(localCache, key, trace)?.let {
                    return hitResponse(key, it.value, it.version, CacheLayer.LOCAL, scenario, podId, it.ttlRemainingSeconds, trace)
                }

                return when (scenario) {
                    CacheScenario.LOCAL_REDIS_DB -> readLocalRedisDb(localCache, podId, key, trace)
                    CacheScenario.LOCAL_REDIS -> readLocalRedis(localCache, podId, key, trace)
                    CacheScenario.LOCAL_DB -> readLocalDb(localCache, podId, key, trace)
                }
            }
        } finally {
            keyLocks.remove(lockKey, lock)
        }
    }

    fun upsertDb(key: String, value: String, publishInvalidation: Boolean = true): CacheMutationResponse {
        val updated = dbMockRepository.upsert(key, value)
        redisMockRepository.put(key, updated.value, updated.version, redisTtlSeconds())
        if (publishInvalidation) {
            invalidateAllLocal(key)
        }

        return CacheMutationResponse(
            key = key,
            value = updated.value,
            version = updated.version,
            message = if (publishInvalidation) {
                "DB updated, Redis refreshed, local caches invalidated"
            } else {
                "DB updated and Redis refreshed, but local caches were not invalidated"
            },
        )
    }

    fun seedRedis(key: String, value: String, ttlSeconds: Long?): CacheMutationResponse {
        val version = redisOnlyVersions.compute(key) { _, current -> (current ?: 0) + 1 } ?: 1
        redisMockRepository.put(key, value, version, ttlSeconds ?: redisTtlSeconds())
        invalidateAllLocal(key)
        return CacheMutationResponse(
            key = key,
            value = value,
            version = version,
            message = "Redis updated and local caches invalidated",
        )
    }

    fun invalidateAllLocal(key: String): CacheInvalidationResponse {
        localCaches.forEach { (_, cache) -> cache.invalidate(key) }
        return CacheInvalidationResponse(key = key, message = "Invalidated local caches for all pods")
    }

    fun clearPodLocalCache(podId: String): CacheInvalidationResponse {
        localCache(podId).invalidateAll()
        return CacheInvalidationResponse(key = null, message = "Local cache cleared for $podId")
    }

    fun clearForTest() {
        localCaches.values.forEach { it.invalidateAll() }
        redisMockRepository.clear()
        dbMockRepository.clear()
        redisOnlyVersions.clear()
    }

    fun getLocalCacheStats(podId: String): CacheStatsResponse {
        val cache = localCache(podId)
        val stats = cache.stats()
        return CacheStatsResponse(
            podId = podId,
            requestCount = stats.requestCount(),
            hitCount = stats.hitCount(),
            missCount = stats.missCount(),
            hitRate = stats.hitRate(),
            evictionCount = stats.evictionCount(),
            estimatedSize = cache.estimatedSize(),
        )
    }

    fun getRedisAccessCount(key: String): Long = redisMockRepository.getAccessCount(key)

    fun getDbAccessCount(key: String): Long = dbMockRepository.getAccessCount(key)

    private fun readLocalRedisDb(
        localCache: Cache<String, LocalCacheEntry>,
        podId: String,
        key: String,
        trace: MutableList<CacheTraceStep>,
    ): CacheLookupResponse {
        readRedis(key, trace)?.let {
            writeLocal(localCache, key, it.value, it.version, trace)
            return hitResponse(key, it.value, it.version, CacheLayer.REDIS, CacheScenario.LOCAL_REDIS_DB, podId, it.ttlRemainingSeconds, trace)
        }

        val db = dbMockRepository.get(key)
        if (db == null) {
            trace += CacheTraceStep(CacheLayer.DB, CacheTraceResult.MISS, "Origin DB miss")
            return missResponse(key, CacheScenario.LOCAL_REDIS_DB, podId, trace)
        }

        trace += CacheTraceStep(CacheLayer.DB, CacheTraceResult.HIT, "Origin DB hit")
        redisMockRepository.put(key, db.value, db.version, redisTtlSeconds())
        trace += CacheTraceStep(CacheLayer.REDIS, CacheTraceResult.WRITE, "Warm up Redis from DB")
        writeLocal(localCache, key, db.value, db.version, trace)
        return hitResponse(key, db.value, db.version, CacheLayer.DB, CacheScenario.LOCAL_REDIS_DB, podId, null, trace)
    }

    private fun readLocalRedis(
        localCache: Cache<String, LocalCacheEntry>,
        podId: String,
        key: String,
        trace: MutableList<CacheTraceStep>,
    ): CacheLookupResponse {
        readRedis(key, trace)?.let {
            writeLocal(localCache, key, it.value, it.version, trace)
            return hitResponse(key, it.value, it.version, CacheLayer.REDIS, CacheScenario.LOCAL_REDIS, podId, it.ttlRemainingSeconds, trace)
        }

        trace += CacheTraceStep(CacheLayer.DB, CacheTraceResult.SKIP, "LOCAL_REDIS scenario does not use DB fallback")
        return missResponse(key, CacheScenario.LOCAL_REDIS, podId, trace)
    }

    private fun readLocalDb(
        localCache: Cache<String, LocalCacheEntry>,
        podId: String,
        key: String,
        trace: MutableList<CacheTraceStep>,
    ): CacheLookupResponse {
        trace += CacheTraceStep(CacheLayer.REDIS, CacheTraceResult.SKIP, "LOCAL_DB scenario does not use Redis")
        val db = dbMockRepository.get(key)
        if (db == null) {
            trace += CacheTraceStep(CacheLayer.DB, CacheTraceResult.MISS, "Origin DB miss")
            return missResponse(key, CacheScenario.LOCAL_DB, podId, trace)
        }

        trace += CacheTraceStep(CacheLayer.DB, CacheTraceResult.HIT, "Origin DB hit")
        writeLocal(localCache, key, db.value, db.version, trace)
        return hitResponse(key, db.value, db.version, CacheLayer.DB, CacheScenario.LOCAL_DB, podId, null, trace)
    }

    private fun readLocal(
        localCache: Cache<String, LocalCacheEntry>,
        key: String,
        trace: MutableList<CacheTraceStep>,
    ): LocalLookupResult? {
        val entry = localCache.getIfPresent(key)
        if (entry == null) {
            trace += CacheTraceStep(CacheLayer.LOCAL, CacheTraceResult.MISS, "Pod local cache miss")
            return null
        }

        val now = System.currentTimeMillis()
        if (entry.expiresAtMillis <= now) {
            localCache.invalidate(key)
            trace += CacheTraceStep(CacheLayer.LOCAL, CacheTraceResult.MISS, "Pod local cache expired")
            return null
        }

        trace += CacheTraceStep(CacheLayer.LOCAL, CacheTraceResult.HIT, "Pod local cache hit")
        return LocalLookupResult(
            value = entry.value,
            version = entry.version,
            ttlRemainingSeconds = ttlRemainingSeconds(entry.expiresAtMillis, now),
        )
    }

    private fun readRedis(key: String, trace: MutableList<CacheTraceStep>): RedisLookupForService? {
        val redis = redisMockRepository.get(key)
        if (redis == null) {
            trace += CacheTraceStep(CacheLayer.REDIS, CacheTraceResult.MISS, "Shared Redis miss")
            return null
        }

        trace += CacheTraceStep(CacheLayer.REDIS, CacheTraceResult.HIT, "Shared Redis hit")
        return RedisLookupForService(
            value = redis.value,
            version = redis.version,
            ttlRemainingSeconds = redis.ttlRemainingSeconds,
        )
    }

    private fun writeLocal(
        localCache: Cache<String, LocalCacheEntry>,
        key: String,
        value: String,
        version: Long,
        trace: MutableList<CacheTraceStep>,
    ) {
        localCache.put(
            key,
            LocalCacheEntry(
                value = value,
                version = version,
                expiresAtMillis = System.currentTimeMillis() + localTtlSeconds() * 1000,
            ),
        )
        trace += CacheTraceStep(CacheLayer.LOCAL, CacheTraceResult.WRITE, "Warm up pod local cache")
    }

    private fun hitResponse(
        key: String,
        value: String,
        version: Long,
        source: CacheLayer,
        scenario: CacheScenario,
        podId: String,
        ttlRemainingSeconds: Long?,
        trace: List<CacheTraceStep>,
    ): CacheLookupResponse {
        return CacheLookupResponse(
            key = key,
            value = value,
            version = version,
            source = source,
            scenario = scenario,
            podId = podId,
            ttlRemainingSeconds = ttlRemainingSeconds,
            trace = trace,
        )
    }

    private fun missResponse(
        key: String,
        scenario: CacheScenario,
        podId: String,
        trace: List<CacheTraceStep>,
    ): CacheLookupResponse {
        return CacheLookupResponse(
            key = key,
            value = null,
            version = null,
            source = CacheLayer.MISS,
            scenario = scenario,
            podId = podId,
            ttlRemainingSeconds = null,
            trace = trace,
        )
    }

    private fun localCache(podId: String): Cache<String, LocalCacheEntry> {
        return localCaches.computeIfAbsent(podId) { newLocalCache() }
    }

    private fun newLocalCache(): Cache<String, LocalCacheEntry> {
        return Caffeine.newBuilder()
            .maximumSize(properties.localMaximumSize)
            .recordStats()
            .expireAfter(object : Expiry<String, LocalCacheEntry> {
                override fun expireAfterCreate(key: String, value: LocalCacheEntry, currentTime: Long): Long {
                    return nanosUntil(value.expiresAtMillis)
                }

                override fun expireAfterUpdate(
                    key: String,
                    value: LocalCacheEntry,
                    currentTime: Long,
                    currentDuration: Long,
                ): Long {
                    return nanosUntil(value.expiresAtMillis)
                }

                override fun expireAfterRead(
                    key: String,
                    value: LocalCacheEntry,
                    currentTime: Long,
                    currentDuration: Long,
                ): Long {
                    return currentDuration
                }
            })
            .build()
    }

    private fun localTtlSeconds(): Long {
        if (properties.ttlJitterSeconds == 0L) {
            return properties.localTtlSeconds
        }
        return properties.localTtlSeconds + ThreadLocalRandom.current().nextLong(properties.ttlJitterSeconds + 1)
    }

    private fun redisTtlSeconds(): Long {
        if (properties.ttlJitterSeconds == 0L) {
            return properties.redisDefaultTtlSeconds
        }
        return properties.redisDefaultTtlSeconds + ThreadLocalRandom.current().nextLong(properties.ttlJitterSeconds + 1)
    }

    private fun nanosUntil(expiresAtMillis: Long): Long {
        val remainingMillis = expiresAtMillis - System.currentTimeMillis()
        return TimeUnit.MILLISECONDS.toNanos(maxOf(1, remainingMillis))
    }

    private fun ttlRemainingSeconds(expiresAtMillis: Long, nowMillis: Long): Long {
        val remainingMillis = expiresAtMillis - nowMillis
        return if (remainingMillis <= 0) 0 else (remainingMillis + 999) / 1000
    }

    private fun podName(index: Int): String = "pod-$index"
}

data class LocalCacheEntry(
    val value: String,
    val version: Long,
    val expiresAtMillis: Long,
)

data class LocalLookupResult(
    val value: String,
    val version: Long,
    val ttlRemainingSeconds: Long,
)

data class RedisLookupForService(
    val value: String,
    val version: Long,
    val ttlRemainingSeconds: Long,
)

data class CacheMutationResponse(
    val key: String,
    val value: String,
    val version: Long,
    val message: String,
)

data class CacheInvalidationResponse(
    val key: String?,
    val message: String,
)

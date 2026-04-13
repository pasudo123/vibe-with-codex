package com.vibewithcodex.study.cachetier.application

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.vibewithcodex.study.cachetier.config.StudyCacheTierProperties
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import com.vibewithcodex.study.cachetier.domain.CacheMutationResponse
import com.vibewithcodex.study.cachetier.domain.CachePattern
import com.vibewithcodex.study.cachetier.domain.CacheSource
import com.vibewithcodex.study.cachetier.domain.CacheStatsResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.springframework.stereotype.Service

/**
 * Caffeine Local Cache의 대표 활용 패턴을 작게 모아둔 실습 서비스.
 */
@Service
class StudyCacheTierService(
    private val properties: StudyCacheTierProperties,
) {
    private val originData = ConcurrentHashMap<String, String>()
    private val cacheAsideLoadCount = AtomicLong()
    private val loadingCacheLoadCount = AtomicLong()
    private val refreshCacheLoadCount = AtomicLong()

    private val cacheAsideCache: Cache<String, String> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(properties.localTtlSeconds))
        .maximumSize(properties.localMaximumSize)
        .recordStats()
        .build()

    private val loadingCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(properties.localTtlSeconds))
        .maximumSize(properties.localMaximumSize)
        .recordStats()
        .build { key -> loadForLoadingCache(key) }

    private val refreshAfterWriteCache: LoadingCache<String, String> = Caffeine.newBuilder()
        .refreshAfterWrite(Duration.ofSeconds(properties.refreshAfterWriteSeconds))
        .expireAfterWrite(Duration.ofSeconds(properties.localTtlSeconds))
        .maximumSize(properties.localMaximumSize)
        .recordStats()
        .build { key -> loadForRefreshCache(key) }

    fun putData(key: String, value: String, invalidateCaches: Boolean = true): CacheMutationResponse {
        originData[key] = value
        if (invalidateCaches) {
            invalidateAllCaches(key)
        }
        return CacheMutationResponse(
            key = key,
            value = value,
            message = if (invalidateCaches) {
                "Origin data saved and Caffeine entries invalidated"
            } else {
                "Origin data saved without invalidating Caffeine entries"
            },
        )
    }

    fun getWithCacheAside(key: String): CacheLookupResponse {
        cacheAsideCache.getIfPresent(key)?.let {
            return CacheLookupResponse(
                key = key,
                value = it,
                pattern = CachePattern.CACHE_ASIDE,
                source = CacheSource.LOCAL_CACHE,
                message = "Cache-aside hit: service code read Caffeine first",
            )
        }

        val loaded = originData[key]
        if (loaded == null) {
            return CacheLookupResponse(
                key = key,
                value = null,
                pattern = CachePattern.CACHE_ASIDE,
                source = CacheSource.MISS,
                message = "Cache-aside miss: origin data does not exist",
            )
        }

        cacheAsideLoadCount.incrementAndGet()
        cacheAsideCache.put(key, loaded)
        return CacheLookupResponse(
            key = key,
            value = loaded,
            pattern = CachePattern.CACHE_ASIDE,
            source = CacheSource.LOADER,
            message = "Cache-aside miss: service loaded origin data and populated Caffeine",
        )
    }

    fun getWithLoadingCache(key: String): CacheLookupResponse {
        val before = loadingCache.getIfPresent(key)
        val value = loadingCache.get(key)
        return CacheLookupResponse(
            key = key,
            value = value,
            pattern = CachePattern.LOADING_CACHE,
            source = if (before == null) CacheSource.LOADER else CacheSource.LOCAL_CACHE,
            message = if (before == null) {
                "LoadingCache miss: Caffeine called the loader automatically"
            } else {
                "LoadingCache hit: Caffeine returned the cached value"
            },
        )
    }

    fun getWithRefreshAfterWrite(key: String): CacheLookupResponse {
        val before = refreshAfterWriteCache.getIfPresent(key)
        val value = refreshAfterWriteCache.get(key)
        return CacheLookupResponse(
            key = key,
            value = value,
            pattern = CachePattern.REFRESH_AFTER_WRITE,
            source = if (before == null) CacheSource.LOADER else CacheSource.LOCAL_CACHE,
            message = if (before == null) {
                "refreshAfterWrite cache miss: Caffeine called the loader"
            } else {
                "refreshAfterWrite cache hit: eligible entries can be refreshed on access"
            },
        )
    }

    fun refreshNow(key: String): CacheMutationResponse {
        val value = refreshAfterWriteCache.refresh(key).get()
        return CacheMutationResponse(
            key = key,
            value = value,
            message = "RefreshAfterWrite cache refreshed from origin data",
        )
    }

    fun invalidate(cacheName: String, key: String): CacheMutationResponse {
        cacheByName(cacheName).invalidate(key)
        return CacheMutationResponse(
            key = key,
            value = null,
            message = "Invalidated $cacheName entry from Caffeine",
        )
    }

    fun getStats(cacheName: String): CacheStatsResponse {
        val cache = cacheByName(cacheName)
        val stats = cache.stats()
        return CacheStatsResponse(
            cacheName = cacheName,
            requestCount = stats.requestCount(),
            hitCount = stats.hitCount(),
            missCount = stats.missCount(),
            hitRate = stats.hitRate(),
            evictionCount = stats.evictionCount(),
            estimatedSize = cache.estimatedSize(),
            loaderCallCount = loaderCount(cacheName),
        )
    }

    fun clearForTest() {
        originData.clear()
        cacheAsideCache.invalidateAll()
        loadingCache.invalidateAll()
        refreshAfterWriteCache.invalidateAll()
        cacheAsideLoadCount.set(0)
        loadingCacheLoadCount.set(0)
        refreshCacheLoadCount.set(0)
    }

    private fun loadForLoadingCache(key: String): String {
        loadingCacheLoadCount.incrementAndGet()
        return originData[key] ?: "generated:$key"
    }

    private fun loadForRefreshCache(key: String): String {
        refreshCacheLoadCount.incrementAndGet()
        return originData[key] ?: "generated:$key"
    }

    private fun invalidateAllCaches(key: String) {
        cacheAsideCache.invalidate(key)
        loadingCache.invalidate(key)
        refreshAfterWriteCache.invalidate(key)
    }

    private fun cacheByName(cacheName: String): Cache<String, String> {
        return when (cacheName.lowercase()) {
            "cache-aside", "cache_aside", "aside" -> cacheAsideCache
            "loading", "loading-cache", "loading_cache" -> loadingCache
            "refresh", "refresh-after-write", "refresh_after_write" -> refreshAfterWriteCache
            else -> throw IllegalArgumentException("Unknown cacheName: $cacheName")
        }
    }

    private fun loaderCount(cacheName: String): Long {
        return when (cacheName.lowercase()) {
            "cache-aside", "cache_aside", "aside" -> cacheAsideLoadCount.get()
            "loading", "loading-cache", "loading_cache" -> loadingCacheLoadCount.get()
            "refresh", "refresh-after-write", "refresh_after_write" -> refreshCacheLoadCount.get()
            else -> throw IllegalArgumentException("Unknown cacheName: $cacheName")
        }
    }
}

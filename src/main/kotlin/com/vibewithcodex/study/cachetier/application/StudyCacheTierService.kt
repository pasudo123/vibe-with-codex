package com.vibewithcodex.study.cachetier.application

import com.github.benmanes.caffeine.cache.Cache
import com.vibewithcodex.study.cachetier.config.StudyCacheTierConfig
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import com.vibewithcodex.study.cachetier.domain.CacheSource
import com.vibewithcodex.study.cachetier.infra.redismock.RedisMockRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StudyCacheTierService(
    @Qualifier(StudyCacheTierConfig.LOCAL_CACHE_BEAN_NAME)
    private val localCache: Cache<String, LocalCacheValue>,
    private val redisMockRepository: RedisMockRepository,
    @Value("\${study.cachetier.local-ttl-seconds:60}")
    private val localTtlSeconds: Long,
    @Value("\${study.cachetier.redis-default-ttl-seconds:60}")
    private val redisDefaultTtlSeconds: Long,
) {

    fun getData(key: String): CacheLookupResponse {
        val now = System.currentTimeMillis()

        localCache.getIfPresent(key)?.let { localValue ->
            val remainingMillis = localValue.expiresAtMillis - now
            val ttlRemainingSeconds = if (remainingMillis <= 0) 0 else (remainingMillis + 999) / 1000
            return CacheLookupResponse(
                key = key,
                value = localValue.value,
                source = CacheSource.LOCAL,
                ttlRemainingSeconds = ttlRemainingSeconds,
            )
        }

        val redisValue = redisMockRepository.get(key)
            ?: return CacheLookupResponse(
                key = key,
                value = null,
                source = CacheSource.MISS,
                ttlRemainingSeconds = null,
            )

        val localExpiresAt = now + (localTtlSeconds * 1000)
        localCache.put(key, LocalCacheValue(redisValue, localExpiresAt))

        return CacheLookupResponse(
            key = key,
            value = redisValue,
            source = CacheSource.REDIS,
            ttlRemainingSeconds = localTtlSeconds,
        )
    }

    fun seedRedis(key: String, value: String, ttlSeconds: Long?) {
        redisMockRepository.put(key, value, ttlSeconds ?: redisDefaultTtlSeconds)
    }

    fun clearForTest() {
        localCache.invalidateAll()
        redisMockRepository.clear()
    }
}

data class LocalCacheValue(
    val value: String,
    val expiresAtMillis: Long,
)

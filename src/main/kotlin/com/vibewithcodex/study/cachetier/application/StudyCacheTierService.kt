package com.vibewithcodex.study.cachetier.application

import com.github.benmanes.caffeine.cache.Cache
import com.vibewithcodex.study.cachetier.config.StudyCacheTierConfig
import com.vibewithcodex.study.cachetier.config.StudyCacheTierProperties
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import com.vibewithcodex.study.cachetier.domain.CacheStatsResponse
import com.vibewithcodex.study.cachetier.domain.CacheSource
import com.vibewithcodex.study.cachetier.infra.redismock.RedisMockRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * 2단계 캐시 조회(Local -> Redis Mock)를 담당하는 서비스.
 *
 * cache-aside 패턴을 수동으로 구현해 동작 원리를 코드에서 직접 확인할 수 있게 한다.
 */
@Service
class StudyCacheTierService(
    @Qualifier(StudyCacheTierConfig.LOCAL_CACHE_BEAN_NAME)
    private val localCache: Cache<String, LocalCacheValue>,
    private val redisMockRepository: RedisMockRepository,
    private val properties: StudyCacheTierProperties,
) {
    private val keyLocks = ConcurrentHashMap<String, Any>()

    /**
     * 조회 순서:
     * 1) Local Cache 조회
     * 2) miss면 Redis Mock 조회
     * 3) Redis hit면 Local Cache에 적재 후 반환
     * 4) 둘 다 miss면 MISS 반환
     */
    fun getData(key: String): CacheLookupResponse {
        // 1차 캐시 hit: 가장 빠른 경로로 즉시 반환한다.
        localCache.getIfPresent(key)?.let { return toLocalHitResponse(key, it) }

        // 동시 miss 폭주를 줄이기 위해 key 단위 single-flight 보호를 건다.
        val lock = keyLocks.computeIfAbsent(key) { Any() }
        try {
            synchronized(lock) {
                // lock 대기 중 누군가 local에 적재했을 수 있으므로 재확인한다.
                localCache.getIfPresent(key)?.let { return toLocalHitResponse(key, it) }

                // 2차 캐시 조회: 로컬 miss일 때만 Redis Mock에 접근한다.
                val redisLookup = redisMockRepository.get(key)
                    ?: return CacheLookupResponse(
                        key = key,
                        value = null,
                        source = CacheSource.MISS,
                        ttlRemainingSeconds = null,
                    )

                // Redis hit 시점에 로컬 캐시를 재적재해 다음 요청을 빠르게 처리한다.
                val redisValue = redisLookup.value
                val localExpiresAt = System.currentTimeMillis() + (properties.localTtlSeconds * 1000)
                localCache.put(key, LocalCacheValue(redisValue, localExpiresAt))

                return CacheLookupResponse(
                    key = key,
                    value = redisValue,
                    source = CacheSource.REDIS,
                    ttlRemainingSeconds = redisLookup.ttlRemainingSeconds,
                )
            }
        } finally {
            keyLocks.remove(key, lock)
        }
    }

    /**
     * 학습/테스트를 위해 Redis Mock 저장소에 seed 데이터를 주입한다.
     */
    fun seedRedis(key: String, value: String, ttlSeconds: Long?) {
        redisMockRepository.put(key, value, ttlSeconds ?: properties.redisDefaultTtlSeconds)
        // 동일 key를 다시 seed하는 경우, 기존 Local 캐시 값/TTL을 즉시 폐기해
        // 다음 조회가 최신 Redis Mock 값을 기준으로 다시 적재되도록 보장한다.
        localCache.invalidate(key)
    }

    /**
     * 테스트 간 데이터 오염 방지를 위한 초기화 메서드.
     */
    fun clearForTest() {
        localCache.invalidateAll()
        redisMockRepository.clear()
    }

    /**
     * 학습 편의용 수동 Local Cache 초기화.
     */
    fun clearLocalCache() {
        localCache.invalidateAll()
    }

    /**
     * local cache 통계 조회.
     */
    fun getLocalCacheStats(): CacheStatsResponse {
        val stats = localCache.stats()
        return CacheStatsResponse(
            requestCount = stats.requestCount(),
            hitCount = stats.hitCount(),
            missCount = stats.missCount(),
            hitRate = stats.hitRate(),
            evictionCount = stats.evictionCount(),
        )
    }

    private fun toLocalHitResponse(key: String, localValue: LocalCacheValue): CacheLookupResponse {
        val now = System.currentTimeMillis()
        return CacheLookupResponse(
            key = key,
            value = localValue.value,
            source = CacheSource.LOCAL,
            ttlRemainingSeconds = calculateTtlRemainingSeconds(localValue.expiresAtMillis, now),
        )
    }

    private fun calculateTtlRemainingSeconds(expiresAtMillis: Long, nowMillis: Long): Long {
        val remainingMillis = expiresAtMillis - nowMillis
        // 나머지 밀리초를 사용자 친화적인 초 단위로 올림 계산한다.
        return if (remainingMillis <= 0) 0 else (remainingMillis + 999) / 1000
    }
}

/**
 * Local Cache 내부 저장 모델.
 * value와 절대 만료 시각을 함께 저장해 남은 TTL 계산에 활용한다.
 */
data class LocalCacheValue(
    val value: String,
    val expiresAtMillis: Long,
)

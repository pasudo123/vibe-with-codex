package com.vibewithcodex.study.cachetier.application

import com.github.benmanes.caffeine.cache.Cache
import com.vibewithcodex.study.cachetier.config.StudyCacheTierConfig
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import com.vibewithcodex.study.cachetier.domain.CacheSource
import com.vibewithcodex.study.cachetier.infra.redismock.RedisMockRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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
    @Value("\${study.cachetier.local-ttl-seconds:60}")
    private val localTtlSeconds: Long,
    @Value("\${study.cachetier.redis-default-ttl-seconds:60}")
    private val redisDefaultTtlSeconds: Long,
) {

    /**
     * 조회 순서:
     * 1) Local Cache 조회
     * 2) miss면 Redis Mock 조회
     * 3) Redis hit면 Local Cache에 적재 후 반환
     * 4) 둘 다 miss면 MISS 반환
     */
    fun getData(key: String): CacheLookupResponse {
        val now = System.currentTimeMillis()

        // 1차 캐시 hit: 가장 빠른 경로로 즉시 반환한다.
        localCache.getIfPresent(key)?.let { localValue ->
            val remainingMillis = localValue.expiresAtMillis - now

            // 나머지 밀리초를 사용자 친화적인 초 단위로 올림 계산한다.
            val ttlRemainingSeconds = if (remainingMillis <= 0) 0 else (remainingMillis + 999) / 1000
            return CacheLookupResponse(
                key = key,
                value = localValue.value,
                source = CacheSource.LOCAL,
                ttlRemainingSeconds = ttlRemainingSeconds,
            )
        }

        // 2차 캐시 조회: 로컬 miss일 때만 Redis Mock에 접근한다.
        val redisValue = redisMockRepository.get(key)
            ?: return CacheLookupResponse(
                key = key,
                value = null,
                source = CacheSource.MISS,
                ttlRemainingSeconds = null,
            )

        // Redis hit 시점에 로컬 캐시를 재적재해 다음 요청을 빠르게 처리한다.
        val localExpiresAt = now + (localTtlSeconds * 1000)
        localCache.put(key, LocalCacheValue(redisValue, localExpiresAt))

        return CacheLookupResponse(
            key = key,
            value = redisValue,
            source = CacheSource.REDIS,
            ttlRemainingSeconds = localTtlSeconds,
        )
    }

    /**
     * 학습/테스트를 위해 Redis Mock 저장소에 seed 데이터를 주입한다.
     */
    fun seedRedis(key: String, value: String, ttlSeconds: Long?) {
        redisMockRepository.put(key, value, ttlSeconds ?: redisDefaultTtlSeconds)
    }

    /**
     * 테스트 간 데이터 오염 방지를 위한 초기화 메서드.
     */
    fun clearForTest() {
        localCache.invalidateAll()
        redisMockRepository.clear()
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

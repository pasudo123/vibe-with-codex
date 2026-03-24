package com.vibewithcodex.study.cachetier.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.vibewithcodex.study.cachetier.application.LocalCacheValue
import java.time.Duration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * study.cachetier 모듈의 로컬 캐시 설정.
 *
 * Caffeine 캐시를 직접 Bean으로 등록해 TTL을 명확히 제어한다.
 * 기본 TTL은 10초이며 프로퍼티로 오버라이드할 수 있다.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(StudyCacheTierProperties::class)
class StudyCacheTierConfig {

    @Bean(name = [LOCAL_CACHE_BEAN_NAME])
    fun studyLocalCache(
        properties: StudyCacheTierProperties,
    ): Cache<String, LocalCacheValue> {
        // expireAfterWrite: 최초 저장 시점부터 TTL 카운트다운.
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(properties.localTtlSeconds))
            // 무제한 성장을 방지하기 위해 캐시 엔트리 상한을 둔다.
            .maximumSize(properties.localMaximumSize)
            // 운영 지표(hit/miss/eviction) 수집을 활성화한다.
            .recordStats()
            .build()
    }

    companion object {
        const val LOCAL_CACHE_BEAN_NAME = "studyLocalCache"
    }
}

package com.vibewithcodex.study.cachetier.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.vibewithcodex.study.cachetier.application.LocalCacheValue
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * study.cachetier 모듈의 로컬 캐시 설정.
 *
 * Caffeine 캐시를 직접 Bean으로 등록해 TTL을 명확히 제어한다.
 * 기본 TTL은 60초이며 프로퍼티로 오버라이드할 수 있다.
 */
@Configuration
@EnableCaching
class StudyCacheTierConfig {

    @Bean(name = [LOCAL_CACHE_BEAN_NAME])
    fun studyLocalCache(
        @Value("\${study.cachetier.local-ttl-seconds:60}") localTtlSeconds: Long,
    ): Cache<String, LocalCacheValue> {
        // expireAfterWrite: 최초 저장 시점부터 TTL 카운트다운.
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(localTtlSeconds))
            .build()
    }

    companion object {
        const val LOCAL_CACHE_BEAN_NAME = "studyLocalCache"
    }
}

package com.vibewithcodex.study.cachetier.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.vibewithcodex.study.cachetier.application.LocalCacheValue
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class StudyCacheTierConfig {

    @Bean(name = [LOCAL_CACHE_BEAN_NAME])
    fun studyLocalCache(
        @Value("\${study.cachetier.local-ttl-seconds:60}") localTtlSeconds: Long,
    ): Cache<String, LocalCacheValue> {
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(localTtlSeconds))
            .build()
    }

    companion object {
        const val LOCAL_CACHE_BEAN_NAME = "studyLocalCache"
    }
}

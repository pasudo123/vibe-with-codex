package com.vibewithcodex.study.cachetier.config

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * cachetier 실습 설정.
 *
 * L1은 pod별 Caffeine local cache, L2는 shared Redis mock, origin은 DB mock으로 둔다.
 */
@Validated
@ConfigurationProperties(prefix = "study.cachetier")
data class StudyCacheTierProperties(
    @field:Positive
    val localTtlSeconds: Long = 10,
    @field:Positive
    val redisDefaultTtlSeconds: Long = 60,
    @field:Positive
    val localMaximumSize: Long = 10_000,
    @field:PositiveOrZero
    val ttlJitterSeconds: Long = 0,
    @field:Positive
    val podCount: Int = 3,
)

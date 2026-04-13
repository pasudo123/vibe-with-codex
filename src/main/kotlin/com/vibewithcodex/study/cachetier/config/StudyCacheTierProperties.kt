package com.vibewithcodex.study.cachetier.config

import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Caffeine 실습 설정.
 */
@Validated
@ConfigurationProperties(prefix = "study.cachetier")
data class StudyCacheTierProperties(
    @field:Positive
    val localTtlSeconds: Long = 10,
    @field:Positive
    val refreshAfterWriteSeconds: Long = 3,
    @field:Positive
    val localMaximumSize: Long = 10_000,
)

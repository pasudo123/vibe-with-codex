package com.vibewithcodex.study.cachetier.config

import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * study.cachetier 설정 바인딩 모델.
 *
 * 값 검증을 통해 잘못된 TTL/크기 설정으로 부팅되는 상황을 방지한다.
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
)

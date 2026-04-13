package com.vibewithcodex.study.cachetier.domain

/**
 * Caffeine cache 통계 응답 모델.
 */
data class CacheStatsResponse(
    val cacheName: String,
    val requestCount: Long,
    val hitCount: Long,
    val missCount: Long,
    val hitRate: Double,
    val evictionCount: Long,
    val estimatedSize: Long,
    val loaderCallCount: Long,
)

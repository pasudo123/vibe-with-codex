package com.vibewithcodex.study.cachetier.domain

/**
 * Local Cache 통계 응답 모델.
 */
data class CacheStatsResponse(
    val requestCount: Long,
    val hitCount: Long,
    val missCount: Long,
    val hitRate: Double,
    val evictionCount: Long,
)

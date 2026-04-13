package com.vibewithcodex.study.cachetier.domain

/**
 * Pod별 Local Cache 통계 응답 모델.
 */
data class CacheStatsResponse(
    val podId: String,
    val requestCount: Long,
    val hitCount: Long,
    val missCount: Long,
    val hitRate: Double,
    val evictionCount: Long,
    val estimatedSize: Long,
)

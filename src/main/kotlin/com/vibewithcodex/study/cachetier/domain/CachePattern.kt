package com.vibewithcodex.study.cachetier.domain

/**
 * Caffeine을 활용하는 대표 패턴.
 */
enum class CachePattern {
    CACHE_ASIDE,
    LOADING_CACHE,
    REFRESH_AFTER_WRITE,
    STATS_INVALIDATE,
}

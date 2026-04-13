package com.vibewithcodex.study.cachetier.domain

/**
 * Caffeine 활용 예제 조회 응답.
 */
data class CacheLookupResponse(
    val key: String,
    val value: String?,
    val pattern: CachePattern,
    val source: CacheSource,
    val message: String,
)

data class CacheMutationResponse(
    val key: String,
    val value: String?,
    val message: String,
)

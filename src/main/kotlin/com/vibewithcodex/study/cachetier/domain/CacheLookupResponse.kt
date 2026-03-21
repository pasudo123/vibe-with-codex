package com.vibewithcodex.study.cachetier.domain

/**
 * 캐시 조회 응답 모델.
 *
 * @property key 조회 대상 키
 * @property value 조회된 값 (MISS인 경우 null)
 * @property source 값이 조회된 계층 (LOCAL / REDIS / MISS)
 * @property ttlRemainingSeconds 로컬 캐시 기준 남은 TTL(초). MISS인 경우 null
 */
data class CacheLookupResponse(
    val key: String,
    val value: String?,
    val source: CacheSource,
    val ttlRemainingSeconds: Long?,
)

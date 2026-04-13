package com.vibewithcodex.study.cachetier.domain

/**
 * 캐시 조회 결과.
 *
 * trace는 어떤 계층을 어떤 순서로 확인했는지 보여주는 학습/운영 관측용 필드다.
 */
data class CacheLookupResponse(
    val key: String,
    val value: String?,
    val version: Long?,
    val source: CacheLayer,
    val scenario: CacheScenario,
    val podId: String,
    val ttlRemainingSeconds: Long?,
    val trace: List<CacheTraceStep>,
)

data class CacheTraceStep(
    val layer: CacheLayer,
    val result: CacheTraceResult,
    val message: String,
)

enum class CacheTraceResult {
    HIT,
    MISS,
    WRITE,
    INVALIDATE,
    SKIP,
}

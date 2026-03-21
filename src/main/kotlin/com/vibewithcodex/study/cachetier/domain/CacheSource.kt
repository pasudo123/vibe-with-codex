package com.vibewithcodex.study.cachetier.domain

/**
 * 조회 결과가 어떤 계층에서 반환되었는지 나타낸다.
 *
 * - LOCAL: 1차 로컬 캐시(Caffeine)에서 즉시 조회 성공
 * - REDIS: 로컬 캐시 miss 후 2차 Redis(Mock) 조회 성공
 * - MISS: 두 계층 모두 데이터 없음
 */
enum class CacheSource {
    LOCAL,
    REDIS,
    MISS,
}

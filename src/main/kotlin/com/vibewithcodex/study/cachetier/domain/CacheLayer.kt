package com.vibewithcodex.study.cachetier.domain

/**
 * 값을 반환했거나 조회에 참여한 캐시/원천 계층.
 */
enum class CacheLayer {
    LOCAL,
    REDIS,
    DB,
    MISS,
}

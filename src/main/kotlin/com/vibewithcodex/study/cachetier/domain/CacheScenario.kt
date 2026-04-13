package com.vibewithcodex.study.cachetier.domain

/**
 * 실무에서 자주 선택하는 read-path cache layer 조합.
 */
enum class CacheScenario {
    LOCAL_REDIS_DB,
    LOCAL_REDIS,
    LOCAL_DB,
}

package com.vibewithcodex.study.cachetier.domain

/**
 * 조회 결과가 어디에서 만들어졌는지 나타낸다.
 */
enum class CacheSource {
    LOCAL_CACHE,
    LOADER,
    MISS,
}

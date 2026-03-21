package com.vibewithcodex.study.cachetier.domain

data class CacheLookupResponse(
    val key: String,
    val value: String?,
    val source: CacheSource,
    val ttlRemainingSeconds: Long?,
)

package com.vibewithcodex.study.cachetier.infra.dbmock

interface DbMockRepository {
    fun get(key: String): DbLookupResult?
    fun upsert(key: String, value: String): DbLookupResult
    fun clear()
    fun getAccessCount(key: String): Long
}

data class DbLookupResult(
    val value: String,
    val version: Long,
)

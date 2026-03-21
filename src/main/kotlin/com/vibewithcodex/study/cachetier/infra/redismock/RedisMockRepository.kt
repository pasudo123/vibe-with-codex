package com.vibewithcodex.study.cachetier.infra.redismock

interface RedisMockRepository {
    fun get(key: String): String?
    fun put(key: String, value: String, ttlSeconds: Long)
    fun clear()
}

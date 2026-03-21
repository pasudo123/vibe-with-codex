package com.vibewithcodex.study.cachetier.infra.redismock

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

/**
 * Redis Mock의 in-memory 구현체.
 *
 * 핵심 포인트:
 * - 저장 시 만료 시각(expiresAtMillis)을 함께 보관한다.
 * - 조회 시 만료 여부를 검사하고 만료면 즉시 제거한다.
 * - ConcurrentHashMap으로 간단한 동시성 접근을 처리한다.
 */
@Repository
class InMemoryRedisMockRepository : RedisMockRepository {

    private val store = ConcurrentHashMap<String, RedisMockEntry>()

    override fun get(key: String): String? {
        val entry = store[key] ?: return null
        val now = System.currentTimeMillis()

        // 조회 시 만료 여부를 판단하는 lazy expiration 방식.
        if (entry.expiresAtMillis <= now) {
            store.remove(key)
            return null
        }
        return entry.value
    }

    override fun put(key: String, value: String, ttlSeconds: Long) {
        require(ttlSeconds > 0) { "ttlSeconds must be greater than 0" }
        val expiresAtMillis = System.currentTimeMillis() + (ttlSeconds * 1000)
        store[key] = RedisMockEntry(value, expiresAtMillis)
    }

    override fun clear() {
        store.clear()
    }
}

/**
 * Redis Mock 내부 저장 모델.
 * value와 절대 만료 시각을 함께 보관한다.
 */
data class RedisMockEntry(
    val value: String,
    val expiresAtMillis: Long,
)

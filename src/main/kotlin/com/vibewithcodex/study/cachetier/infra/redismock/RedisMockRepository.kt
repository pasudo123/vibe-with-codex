package com.vibewithcodex.study.cachetier.infra.redismock

/**
 * 실제 Redis를 대체하는 학습용 저장소 인터페이스.
 *
 * 구현체는 TTL 만료 처리까지 포함해 "Redis가 있다고 가정한" 동작을 흉내 낸다.
 */
interface RedisMockRepository {
    /**
     * key에 해당하는 값을 조회한다.
     * 값이 없거나 TTL이 만료된 경우 null을 반환한다.
     */
    fun get(key: String): String?

    /**
     * key/value를 TTL(초)과 함께 저장한다.
     */
    fun put(key: String, value: String, ttlSeconds: Long)

    /**
     * 테스트 격리를 위해 내부 저장소를 초기화한다.
     */
    fun clear()
}

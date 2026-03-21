package com.vibewithcodex.study.cachetier.api

import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * cache-tier 학습용 API.
 *
 * - seed: Redis Mock에 데이터 주입
 * - data: Local -> Redis 순서의 실제 조회 흐름 확인
 */
@RestController
@RequestMapping("/study/cachetier")
class StudyCacheTierController(
    private val studyCacheTierService: StudyCacheTierService,
) {

    /**
     * Redis Mock에 seed 데이터를 저장한다.
     * ttlSeconds를 생략하면 서비스 기본 TTL 정책을 사용한다.
     */
    @PostMapping("/seed")
    fun seed(@RequestBody request: SeedCacheRequest): SeedCacheResponse {
        studyCacheTierService.seedRedis(
            key = request.key,
            value = request.value,
            ttlSeconds = request.ttlSeconds,
        )

        return SeedCacheResponse(
            key = request.key,
            value = request.value,
            ttlSeconds = request.ttlSeconds,
            message = "Seeded into Redis mock",
        )
    }

    /**
     * 키를 조회하고 어떤 계층에서 hit 되었는지(source)를 함께 반환한다.
     */
    @GetMapping("/data/{key}")
    fun getData(@PathVariable key: String): CacheLookupResponse {
        return studyCacheTierService.getData(key)
    }
}

/**
 * seed 요청 모델.
 *
 * @property key 캐시 키
 * @property value 저장할 값
 * @property ttlSeconds Redis Mock TTL(초), null이면 서비스 기본값 사용
 */
data class SeedCacheRequest(
    @field:NotBlank
    val key: String,
    @field:NotBlank
    val value: String,
    @field:Positive
    val ttlSeconds: Long? = null,
)

/**
 * seed 결과 응답 모델.
 */
data class SeedCacheResponse(
    val key: String,
    val value: String,
    val ttlSeconds: Long?,
    val message: String,
)

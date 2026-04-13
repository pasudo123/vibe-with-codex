package com.vibewithcodex.study.cachetier.api

import com.vibewithcodex.study.cachetier.application.CacheInvalidationResponse
import com.vibewithcodex.study.cachetier.application.CacheMutationResponse
import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import com.vibewithcodex.study.cachetier.domain.CacheScenario
import com.vibewithcodex.study.cachetier.domain.CacheStatsResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 실무형 cache layer 학습 API.
 */
@RestController
@RequestMapping("/study/cachetier")
class StudyCacheTierController(
    private val studyCacheTierService: StudyCacheTierService,
) {

    @PostMapping("/db/{key}")
    fun upsertDb(
        @PathVariable key: String,
        @Valid @RequestBody request: UpsertDbRequest,
    ): CacheMutationResponse {
        return studyCacheTierService.upsertDb(
            key = key,
            value = request.value,
            publishInvalidation = request.publishInvalidation,
        )
    }

    @PostMapping("/redis/{key}")
    fun seedRedis(
        @PathVariable key: String,
        @Valid @RequestBody request: SeedRedisRequest,
    ): CacheMutationResponse {
        return studyCacheTierService.seedRedis(
            key = key,
            value = request.value,
            ttlSeconds = request.ttlSeconds,
        )
    }

    @GetMapping("/{scenario}/pods/{podId}/data/{key}")
    fun getData(
        @PathVariable scenario: CacheScenario,
        @PathVariable podId: String,
        @PathVariable key: String,
    ): CacheLookupResponse {
        return studyCacheTierService.getData(scenario, podId, key)
    }

    @PostMapping("/invalidate/{key}")
    fun invalidateAllLocal(@PathVariable key: String): CacheInvalidationResponse {
        return studyCacheTierService.invalidateAllLocal(key)
    }

    @PostMapping("/pods/{podId}/local/clear")
    fun clearPodLocalCache(@PathVariable podId: String): CacheInvalidationResponse {
        return studyCacheTierService.clearPodLocalCache(podId)
    }

    @GetMapping("/pods/{podId}/stats")
    fun getPodStats(@PathVariable podId: String): CacheStatsResponse {
        return studyCacheTierService.getLocalCacheStats(podId)
    }
}

data class UpsertDbRequest(
    @field:NotBlank
    val value: String,
    val publishInvalidation: Boolean = true,
)

data class SeedRedisRequest(
    @field:NotBlank
    val value: String,
    @field:Positive
    val ttlSeconds: Long? = null,
)

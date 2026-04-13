package com.vibewithcodex.study.cachetier.api

import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CacheLookupResponse
import com.vibewithcodex.study.cachetier.domain.CacheMutationResponse
import com.vibewithcodex.study.cachetier.domain.CacheStatsResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Caffeine 활용 패턴 실습 API.
 */
@RestController
@RequestMapping("/study/cachetier")
class StudyCacheTierController(
    private val studyCacheTierService: StudyCacheTierService,
) {

    @PostMapping("/data/{key}")
    fun putData(
        @PathVariable key: String,
        @Valid @RequestBody request: PutDataRequest,
    ): CacheMutationResponse {
        return studyCacheTierService.putData(
            key = key,
            value = request.value,
            invalidateCaches = request.invalidateCaches,
        )
    }

    @GetMapping("/cache-aside/{key}")
    fun getWithCacheAside(@PathVariable key: String): CacheLookupResponse {
        return studyCacheTierService.getWithCacheAside(key)
    }

    @GetMapping("/loading/{key}")
    fun getWithLoadingCache(@PathVariable key: String): CacheLookupResponse {
        return studyCacheTierService.getWithLoadingCache(key)
    }

    @GetMapping("/refresh/{key}")
    fun getWithRefreshAfterWrite(@PathVariable key: String): CacheLookupResponse {
        return studyCacheTierService.getWithRefreshAfterWrite(key)
    }

    @PostMapping("/refresh/{key}")
    fun refreshNow(@PathVariable key: String): CacheMutationResponse {
        return studyCacheTierService.refreshNow(key)
    }

    @PostMapping("/{cacheName}/{key}/invalidate")
    fun invalidate(
        @PathVariable cacheName: String,
        @PathVariable key: String,
    ): CacheMutationResponse {
        return studyCacheTierService.invalidate(cacheName, key)
    }

    @GetMapping("/{cacheName}/stats")
    fun getStats(@PathVariable cacheName: String): CacheStatsResponse {
        return studyCacheTierService.getStats(cacheName)
    }
}

data class PutDataRequest(
    @field:NotBlank
    val value: String,
    val invalidateCaches: Boolean = true,
)

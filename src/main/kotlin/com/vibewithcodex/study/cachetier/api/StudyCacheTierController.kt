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

@RestController
@RequestMapping("/study/cachetier")
class StudyCacheTierController(
    private val studyCacheTierService: StudyCacheTierService,
) {

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

    @GetMapping("/data/{key}")
    fun getData(@PathVariable key: String): CacheLookupResponse {
        return studyCacheTierService.getData(key)
    }
}

data class SeedCacheRequest(
    @field:NotBlank
    val key: String,
    @field:NotBlank
    val value: String,
    @field:Positive
    val ttlSeconds: Long? = null,
)

data class SeedCacheResponse(
    val key: String,
    val value: String,
    val ttlSeconds: Long?,
    val message: String,
)

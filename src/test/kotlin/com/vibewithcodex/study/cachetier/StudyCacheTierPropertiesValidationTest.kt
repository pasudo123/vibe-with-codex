package com.vibewithcodex.study.cachetier

import com.vibewithcodex.study.cachetier.config.StudyCacheTierProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class StudyCacheTierPropertiesValidationTest : FunSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    test("ttl, maximum size, and pod count must be valid") {
        val properties = StudyCacheTierProperties(
            localTtlSeconds = 0,
            redisDefaultTtlSeconds = -1,
            localMaximumSize = 0,
            ttlJitterSeconds = -1,
            podCount = 0,
        )

        val violations = validator.validate(properties)
        violations.size shouldBe 5
        val fields = violations.map { it.propertyPath.toString() }
        fields shouldContain "localTtlSeconds"
        fields shouldContain "redisDefaultTtlSeconds"
        fields shouldContain "localMaximumSize"
        fields shouldContain "ttlJitterSeconds"
        fields shouldContain "podCount"
    }
})

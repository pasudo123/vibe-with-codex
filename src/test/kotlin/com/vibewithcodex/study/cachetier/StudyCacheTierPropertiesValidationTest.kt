package com.vibewithcodex.study.cachetier

import com.vibewithcodex.study.cachetier.config.StudyCacheTierProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class StudyCacheTierPropertiesValidationTest : FunSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    test("ttl, refresh interval, and maximum size must be positive") {
        val properties = StudyCacheTierProperties(
            localTtlSeconds = 0,
            refreshAfterWriteSeconds = -1,
            localMaximumSize = 0,
        )

        val violations = validator.validate(properties)
        violations.size shouldBe 3
        val fields = violations.map { it.propertyPath.toString() }
        fields shouldContain "localTtlSeconds"
        fields shouldContain "refreshAfterWriteSeconds"
        fields shouldContain "localMaximumSize"
    }
})

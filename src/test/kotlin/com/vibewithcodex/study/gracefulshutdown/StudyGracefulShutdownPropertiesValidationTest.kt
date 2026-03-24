package com.vibewithcodex.study.gracefulshutdown

import com.vibewithcodex.study.gracefulshutdown.config.StudyGracefulShutdownProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class StudyGracefulShutdownPropertiesValidationTest : FunSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    test("graceful shutdown 학습 프로퍼티는 유효 범위를 만족해야 한다") {
        val properties = StudyGracefulShutdownProperties(
            defaultTimeoutMs = 0,
            defaultRetries = -1,
            warmupDelayMs = 0,
            drainingDelayMs = 0,
            readyDelayMs = 0,
            retryBackoffMs = -1,
        )

        val violations = validator.validate(properties)
        violations.size shouldBe 6

        val fields = violations.map { it.propertyPath.toString() }
        fields shouldContain "defaultTimeoutMs"
        fields shouldContain "defaultRetries"
        fields shouldContain "warmupDelayMs"
        fields shouldContain "drainingDelayMs"
        fields shouldContain "readyDelayMs"
        fields shouldContain "retryBackoffMs"
    }
})

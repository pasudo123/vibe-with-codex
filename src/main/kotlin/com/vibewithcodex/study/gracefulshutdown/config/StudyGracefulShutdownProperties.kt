package com.vibewithcodex.study.gracefulshutdown.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "study.graceful-shutdown")
data class StudyGracefulShutdownProperties(
    @field:Positive
    val defaultTimeoutMs: Long = 250,
    @field:Min(0)
    val defaultRetries: Int = 1,
    @field:Positive
    val warmupDelayMs: Long = 1_200,
    @field:Positive
    val drainingDelayMs: Long = 900,
    @field:Positive
    val readyDelayMs: Long = 30,
    @field:Min(0)
    val retryBackoffMs: Long = 50,
)

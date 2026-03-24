package com.vibewithcodex.study.gracefulshutdown.domain

enum class ScenarioKey {
    READY_BASELINE,
    WARMUP_MISS,
    DRAINING_POD_HIT,
    RETRY_RECOVERS,
    ;

    companion object {
        fun from(raw: String): ScenarioKey {
            val normalized = raw.trim().replace('-', '_')
            return entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
                ?: throw IllegalArgumentException("지원하지 않는 시나리오입니다: $raw")
        }
    }
}

enum class BPodState {
    WARMING_UP,
    READY,
    DRAINING,
    TERMINATED,
}

enum class ACallResult {
    SUCCESS,
    TIMEOUT,
    FAILED,
}

data class ScenarioAttemptTraceResponse(
    val attempt: Int,
    val bStateAtCall: BPodState,
    val result: ACallResult,
    val timeoutOccurred: Boolean,
    val elapsedMs: Long,
    val detail: String,
)

data class ScenarioRunResponse(
    val scenarioKey: ScenarioKey,
    val aCallResult: ACallResult,
    val timeoutOccurred: Boolean,
    val bStateAtCall: BPodState,
    val elapsedMs: Long,
    val hint: String,
    val attempts: List<ScenarioAttemptTraceResponse>,
)

data class GracefulShutdownStateResponse(
    val currentBState: BPodState,
    val defaultTimeoutMs: Long,
    val defaultRetries: Int,
    val warmupDelayMs: Long,
    val drainingDelayMs: Long,
    val readyDelayMs: Long,
    val retryBackoffMs: Long,
    val lastScenarioKey: ScenarioKey?,
    val lastResult: ACallResult?,
    val lastRunAtEpochMs: Long?,
)

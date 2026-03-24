package com.vibewithcodex.study.gracefulshutdown.application

import com.vibewithcodex.study.gracefulshutdown.config.StudyGracefulShutdownConfig
import com.vibewithcodex.study.gracefulshutdown.config.StudyGracefulShutdownProperties
import com.vibewithcodex.study.gracefulshutdown.domain.ACallResult
import com.vibewithcodex.study.gracefulshutdown.domain.BPodState
import com.vibewithcodex.study.gracefulshutdown.domain.GracefulShutdownStateResponse
import com.vibewithcodex.study.gracefulshutdown.domain.ScenarioAttemptTraceResponse
import com.vibewithcodex.study.gracefulshutdown.domain.ScenarioKey
import com.vibewithcodex.study.gracefulshutdown.domain.ScenarioRunResponse
import java.time.Clock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * A -> B 호출에서 rollout 타이밍 이슈를 단일 앱 안에서 재현하기 위한 학습 서비스.
 */
@Service
class StudyGracefulShutdownService(
    private val properties: StudyGracefulShutdownProperties,
    @Qualifier(StudyGracefulShutdownConfig.CALL_EXECUTOR_BEAN_NAME)
    private val callExecutor: Executor,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val currentBState = AtomicReference(BPodState.READY)
    private val lastRun = AtomicReference<LastRunSnapshot?>(null)

    fun runScenario(
        scenarioKey: ScenarioKey,
        timeoutMsOverride: Long?,
        retriesOverride: Int?,
    ): ScenarioRunResponse {
        val timeoutMs = timeoutMsOverride ?: properties.defaultTimeoutMs
        val retries = retriesOverride ?: properties.defaultRetries

        require(timeoutMs > 0) { "timeoutMs는 1 이상이어야 합니다." }
        require(retries >= 0) { "retries는 0 이상이어야 합니다." }

        val scenarioPlan = buildScenarioPlan(scenarioKey)
        val attempts = mutableListOf<ScenarioAttemptTraceResponse>()
        val startedAt = System.currentTimeMillis()
        var finalResult = ACallResult.FAILED
        var timeoutOccurred = false
        var finalDetail = "요청이 처리되지 않았습니다."

        val maxAttempts = retries + 1
        for (attempt in 1..maxAttempts) {
            val bState = scenarioPlan.stateForAttempt(attempt)
            currentBState.set(bState)

            val outcome = callBWithTimeout(bState, timeoutMs)
            attempts += ScenarioAttemptTraceResponse(
                attempt = attempt,
                bStateAtCall = bState,
                result = outcome.result,
                timeoutOccurred = outcome.timeoutOccurred,
                elapsedMs = outcome.elapsedMs,
                detail = outcome.detail,
            )

            if (outcome.timeoutOccurred) {
                timeoutOccurred = true
            }

            finalResult = outcome.result
            finalDetail = outcome.detail

            if (outcome.result == ACallResult.SUCCESS) {
                break
            }

            if (attempt < maxAttempts && properties.retryBackoffMs > 0) {
                sleep(properties.retryBackoffMs)
            }
        }

        val elapsedMs = System.currentTimeMillis() - startedAt
        val response = ScenarioRunResponse(
            scenarioKey = scenarioKey,
            aCallResult = finalResult,
            timeoutOccurred = timeoutOccurred,
            bStateAtCall = attempts.firstOrNull()?.bStateAtCall ?: currentBState.get(),
            elapsedMs = elapsedMs,
            hint = "${scenarioPlan.hint} | 최종 상세: $finalDetail",
            attempts = attempts,
        )

        lastRun.set(
            LastRunSnapshot(
                scenarioKey = scenarioKey,
                result = finalResult,
                runAtEpochMs = clock.millis(),
            ),
        )

        return response
    }

    fun currentState(): GracefulShutdownStateResponse {
        val snapshot = lastRun.get()
        return GracefulShutdownStateResponse(
            currentBState = currentBState.get(),
            defaultTimeoutMs = properties.defaultTimeoutMs,
            defaultRetries = properties.defaultRetries,
            warmupDelayMs = properties.warmupDelayMs,
            drainingDelayMs = properties.drainingDelayMs,
            readyDelayMs = properties.readyDelayMs,
            retryBackoffMs = properties.retryBackoffMs,
            lastScenarioKey = snapshot?.scenarioKey,
            lastResult = snapshot?.result,
            lastRunAtEpochMs = snapshot?.runAtEpochMs,
        )
    }

    private fun buildScenarioPlan(scenarioKey: ScenarioKey): ScenarioPlan {
        return when (scenarioKey) {
            ScenarioKey.READY_BASELINE -> ScenarioPlan(
                states = listOf(BPodState.READY),
                hint = "B가 READY 이후 엔드포인트에 진입한 정상 케이스",
            )

            ScenarioKey.WARMUP_MISS -> ScenarioPlan(
                states = listOf(BPodState.WARMING_UP),
                hint = "웜업 완료 전에 트래픽이 유입되면 A 타임아웃이 발생할 수 있음",
            )

            ScenarioKey.DRAINING_POD_HIT -> ScenarioPlan(
                states = listOf(BPodState.DRAINING),
                hint = "종료 중(draining) pod로 호출이 들어가면 타임아웃/실패가 발생할 수 있음",
            )

            ScenarioKey.RETRY_RECOVERS -> ScenarioPlan(
                states = listOf(BPodState.DRAINING, BPodState.READY),
                hint = "첫 시도는 draining pod에서 실패, 재시도로 READY pod에서 회복",
            )
        }
    }

    private fun callBWithTimeout(
        bState: BPodState,
        timeoutMs: Long,
    ): AttemptOutcome {
        val startedNs = System.nanoTime()
        val future = CompletableFuture.supplyAsync(
            { simulateBResponse(bState) },
            callExecutor,
        )

        return try {
            val response = future.get(timeoutMs, TimeUnit.MILLISECONDS)
            AttemptOutcome(
                result = ACallResult.SUCCESS,
                timeoutOccurred = false,
                elapsedMs = elapsedSince(startedNs),
                detail = response,
            )
        } catch (_: TimeoutException) {
            future.cancel(true)
            AttemptOutcome(
                result = ACallResult.TIMEOUT,
                timeoutOccurred = true,
                elapsedMs = elapsedSince(startedNs),
                detail = "A timeout($timeoutMs ms) - B state=$bState",
            )
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            AttemptOutcome(
                result = ACallResult.FAILED,
                timeoutOccurred = false,
                elapsedMs = elapsedSince(startedNs),
                detail = "인터럽트로 호출 중단: ${ex.message}",
            )
        } catch (ex: Exception) {
            AttemptOutcome(
                result = ACallResult.FAILED,
                timeoutOccurred = false,
                elapsedMs = elapsedSince(startedNs),
                detail = rootMessage(ex),
            )
        }
    }

    private fun simulateBResponse(bState: BPodState): String {
        return when (bState) {
            BPodState.WARMING_UP -> {
                sleep(properties.warmupDelayMs)
                "B responded after warmup delay"
            }

            BPodState.READY -> {
                sleep(properties.readyDelayMs)
                "B READY 200 OK"
            }

            BPodState.DRAINING -> {
                sleep(properties.drainingDelayMs)
                error("B pod is draining and cannot finish the request")
            }

            BPodState.TERMINATED -> {
                error("B pod already terminated")
            }
        }
    }

    private fun sleep(delayMs: Long) {
        try {
            Thread.sleep(delayMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun elapsedSince(startedNs: Long): Long {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNs)
    }

    private fun rootMessage(ex: Exception): String {
        val cause = ex.cause
        return cause?.message ?: ex.message ?: ex::class.simpleName.orEmpty()
    }

    private data class ScenarioPlan(
        val states: List<BPodState>,
        val hint: String,
    ) {
        fun stateForAttempt(attempt: Int): BPodState {
            val index = attempt - 1
            return states.getOrElse(index) { states.last() }
        }
    }

    private data class AttemptOutcome(
        val result: ACallResult,
        val timeoutOccurred: Boolean,
        val elapsedMs: Long,
        val detail: String,
    )

    private data class LastRunSnapshot(
        val scenarioKey: ScenarioKey,
        val result: ACallResult,
        val runAtEpochMs: Long,
    )
}

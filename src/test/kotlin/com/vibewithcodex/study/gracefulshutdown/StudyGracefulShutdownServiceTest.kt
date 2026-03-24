package com.vibewithcodex.study.gracefulshutdown

import com.vibewithcodex.study.gracefulshutdown.application.StudyGracefulShutdownService
import com.vibewithcodex.study.gracefulshutdown.domain.ACallResult
import com.vibewithcodex.study.gracefulshutdown.domain.BPodState
import com.vibewithcodex.study.gracefulshutdown.domain.ScenarioKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class StudyGracefulShutdownServiceTest : FunSpec() {

    @Autowired
    lateinit var service: StudyGracefulShutdownService

    init {
        extensions(SpringExtension)

        test("READY baseline 시나리오는 성공한다") {
            val response = service.runScenario(
                scenarioKey = ScenarioKey.READY_BASELINE,
                timeoutMsOverride = 300,
                retriesOverride = 0,
            )

            response.aCallResult shouldBe ACallResult.SUCCESS
            response.timeoutOccurred shouldBe false
            response.bStateAtCall shouldBe BPodState.READY
            response.attempts.shouldHaveSize(1)
        }

        test("warmup miss 시나리오는 timeout을 재현한다") {
            val response = service.runScenario(
                scenarioKey = ScenarioKey.WARMUP_MISS,
                timeoutMsOverride = 100,
                retriesOverride = 0,
            )

            response.aCallResult shouldBe ACallResult.TIMEOUT
            response.timeoutOccurred shouldBe true
            response.bStateAtCall shouldBe BPodState.WARMING_UP
        }

        test("draining pod hit 시나리오는 timeout을 재현한다") {
            val response = service.runScenario(
                scenarioKey = ScenarioKey.DRAINING_POD_HIT,
                timeoutMsOverride = 100,
                retriesOverride = 0,
            )

            response.aCallResult shouldBe ACallResult.TIMEOUT
            response.timeoutOccurred shouldBe true
            response.bStateAtCall shouldBe BPodState.DRAINING
        }

        test("retry recovers 시나리오는 재시도로 회복한다") {
            val response = service.runScenario(
                scenarioKey = ScenarioKey.RETRY_RECOVERS,
                timeoutMsOverride = 120,
                retriesOverride = 1,
            )

            response.aCallResult shouldBe ACallResult.SUCCESS
            response.timeoutOccurred shouldBe true
            response.attempts.shouldHaveSize(2)
            response.attempts[0].result shouldBe ACallResult.TIMEOUT
            response.attempts[1].result shouldBe ACallResult.SUCCESS
        }

        test("state API 모델에는 마지막 실행 정보가 반영된다") {
            service.runScenario(
                scenarioKey = ScenarioKey.READY_BASELINE,
                timeoutMsOverride = 300,
                retriesOverride = 0,
            )

            val state = service.currentState()
            state.currentBState shouldBe BPodState.READY
            state.lastScenarioKey shouldBe ScenarioKey.READY_BASELINE
            state.lastResult shouldBe ACallResult.SUCCESS
        }
    }
}

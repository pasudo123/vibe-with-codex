package com.vibewithcodex.study.gracefulshutdown.api

import com.vibewithcodex.study.gracefulshutdown.application.StudyGracefulShutdownService
import com.vibewithcodex.study.gracefulshutdown.domain.GracefulShutdownStateResponse
import com.vibewithcodex.study.gracefulshutdown.domain.ScenarioKey
import com.vibewithcodex.study.gracefulshutdown.domain.ScenarioRunResponse
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/study/graceful-shutdown")
class StudyGracefulShutdownController(
    private val studyGracefulShutdownService: StudyGracefulShutdownService,
) {

    @PostMapping("/scenarios/{scenarioKey}/run")
    fun runScenario(
        @PathVariable scenarioKey: String,
        @RequestParam(required = false) timeoutMs: Long?,
        @RequestParam(required = false) retries: Int?,
    ): ScenarioRunResponse {
        val parsedScenarioKey = try {
            ScenarioKey.from(scenarioKey)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        }

        return studyGracefulShutdownService.runScenario(
            scenarioKey = parsedScenarioKey,
            timeoutMsOverride = timeoutMs,
            retriesOverride = retries,
        )
    }

    @GetMapping("/state")
    fun state(): GracefulShutdownStateResponse {
        return studyGracefulShutdownService.currentState()
    }
}

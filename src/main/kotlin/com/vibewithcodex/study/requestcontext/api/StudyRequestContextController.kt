package com.vibewithcodex.study.requestcontext.api

import com.vibewithcodex.study.requestcontext.application.StudyRequestContextService
import com.vibewithcodex.study.requestcontext.domain.AsyncRequestContextResponse
import com.vibewithcodex.study.requestcontext.domain.CoroutineRequestContextResponse
import com.vibewithcodex.study.requestcontext.domain.CurrentRequestContextResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * RequestContextHolder 학습용 API.
 *
 * - current: 현재 요청 스레드에서의 컨텍스트 조회
 * - async: 별도 스레드에서 RequestContextHolder 누락 확인
 * - coroutine: coroutine 컨텍스트 전환 시 동작 확인
 */
@RestController
@RequestMapping("/study/request-context")
class StudyRequestContextController(
    private val studyRequestContextService: StudyRequestContextService,
) {

    @GetMapping("/current")
    fun current(): CurrentRequestContextResponse {
        return studyRequestContextService.inspectCurrentRequestContext()
    }

    @GetMapping("/async")
    fun async(): AsyncRequestContextResponse {
        return studyRequestContextService.inspectAsyncContext()
    }

    @GetMapping("/coroutine")
    suspend fun coroutine(): CoroutineRequestContextResponse {
        return studyRequestContextService.inspectCoroutineContext()
    }
}


package com.vibewithcodex.study.requestcontext

import com.vibewithcodex.study.requestcontext.application.StudyRequestContextService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@SpringBootTest
class StudyRequestContextServiceTest : FunSpec() {

    @Autowired
    lateinit var studyRequestContextService: StudyRequestContextService

    private var boundAttributes: ServletRequestAttributes? = null

    init {
        extensions(SpringExtension)

        beforeTest {
            clearRequestContext()
        }

        afterTest {
            clearRequestContext()
        }

        test("현재 요청 스레드에서는 RequestContextHolder 값을 정상 조회한다") {
            bindRequest(traceId = "trace-current")

            val response = studyRequestContextService.inspectCurrentRequestContext()

            response.traceId shouldBe "trace-current"
            response.method shouldBe "GET"
            response.requestUri shouldBe "/study/request-context/current"
            response.clientIp shouldBe "127.0.0.1"
            response.userAgent shouldBe "kotest-agent"
        }

        test("별도 worker 스레드에서는 RequestContextHolder가 비어있고 명시 전달 값은 유지된다") {
            bindRequest(traceId = "trace-async")

            val response = studyRequestContextService.inspectAsyncContext()

            response.requestTraceId shouldBe "trace-async"
            response.workerThreadName.shouldStartWith("study-request-context-")
            response.traceIdFromRequestContextHolderInWorker.shouldBeNull()
            response.traceIdFromExplicitArgumentInWorker shouldBe "trace-async"
        }

        test("코루틴 컨텍스트 전환 시 RequestContextHolder는 비어있고 명시 전달은 유지된다") {
            bindRequest(traceId = "trace-coroutine")

            val response = runBlocking {
                studyRequestContextService.inspectCoroutineContext()
            }

            response.requestTraceId shouldBe "trace-coroutine"
            response.switchedThreadName.shouldStartWith("study-request-context-")
            response.traceIdFromRequestContextHolderInSwitchedCoroutine.shouldBeNull()
            response.traceIdFromExplicitArgumentInSwitchedCoroutine shouldBe "trace-coroutine"
            response.traceIdFromThreadLocalContextElement shouldBe "trace-coroutine"
        }

        test("요청 컨텍스트 없이 currentRequestAttributes 접근 시 예외가 발생한다") {
            shouldThrow<IllegalStateException> {
                studyRequestContextService.inspectCurrentRequestContext()
            }
        }
    }

    private fun bindRequest(traceId: String) {
        val request = MockHttpServletRequest("GET", "/study/request-context/current")
        request.remoteAddr = "127.0.0.1"
        request.addHeader("X-Request-Id", traceId)
        request.addHeader("User-Agent", "kotest-agent")

        val attributes = ServletRequestAttributes(request)
        RequestContextHolder.setRequestAttributes(attributes)
        boundAttributes = attributes
    }

    private fun clearRequestContext() {
        boundAttributes?.requestCompleted()
        boundAttributes = null
        RequestContextHolder.resetRequestAttributes()
    }
}


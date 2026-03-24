package com.vibewithcodex.study.requestcontext.application

import com.vibewithcodex.study.requestcontext.config.StudyRequestContextConfig
import com.vibewithcodex.study.requestcontext.domain.AsyncRequestContextResponse
import com.vibewithcodex.study.requestcontext.domain.CoroutineRequestContextResponse
import com.vibewithcodex.study.requestcontext.domain.CurrentRequestContextResponse
import com.vibewithcodex.study.requestcontext.domain.RequestContextSnapshot
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * RequestContextHolder 동작을 학습하기 위한 서비스.
 *
 * - 현재 요청 스레드에서의 정상 동작
 * - 별도 스레드 실행 시 context 누락
 * - Coroutine 컨텍스트 전환에서의 주의사항
 */
@Service
class StudyRequestContextService(
    @Qualifier(StudyRequestContextConfig.ASYNC_EXECUTOR_BEAN_NAME)
    private val asyncExecutor: Executor,
) {

    // 전용 비동기 executor를 coroutine dispatcher로 사용해 스레드 전환을 명확히 재현한다.
    private val coroutineDispatcher = asyncExecutor.asCoroutineDispatcher()

    fun inspectCurrentRequestContext(): CurrentRequestContextResponse {
        val snapshot = requireCurrentSnapshot()
        return CurrentRequestContextResponse(
            requestThreadName = snapshot.threadName,
            traceId = snapshot.traceId,
            method = snapshot.method,
            requestUri = snapshot.requestUri,
            clientIp = snapshot.clientIp,
            userAgent = snapshot.userAgent,
        )
    }

    fun inspectAsyncContext(): AsyncRequestContextResponse {
        val requestSnapshot = requireCurrentSnapshot()

        val workerProbe = CompletableFuture.supplyAsync(
            {
                val workerSnapshot = currentSnapshotOrNull()
                AsyncWorkerProbe(
                    workerThreadName = Thread.currentThread().name,
                    traceIdFromRequestContextHolder = workerSnapshot?.traceId,
                    traceIdFromExplicitArgument = requestSnapshot.traceId,
                )
            },
            asyncExecutor,
        ).get()

        return AsyncRequestContextResponse(
            requestThreadName = requestSnapshot.threadName,
            requestTraceId = requestSnapshot.traceId,
            workerThreadName = workerProbe.workerThreadName,
            traceIdFromRequestContextHolderInWorker = workerProbe.traceIdFromRequestContextHolder,
            traceIdFromExplicitArgumentInWorker = workerProbe.traceIdFromExplicitArgument,
        )
    }

    suspend fun inspectCoroutineContext(): CoroutineRequestContextResponse {
        val requestSnapshot = requireCurrentSnapshot()

        val switchedWithoutPropagation = withContext(coroutineDispatcher) {
            CoroutineThreadProbe(
                switchedThreadName = Thread.currentThread().name,
                traceIdFromRequestContextHolder = currentSnapshotOrNull()?.traceId,
            )
        }

        val traceIdFromExplicitArgument = withContext(coroutineDispatcher) {
            requestSnapshot.traceId
        }

        val traceIdThreadLocal = ThreadLocal<String?>()
        val traceIdFromThreadLocalContextElement = withContext(
            coroutineDispatcher + traceIdThreadLocal.asContextElement(requestSnapshot.traceId),
        ) {
            traceIdThreadLocal.get()
        }

        return CoroutineRequestContextResponse(
            requestThreadName = requestSnapshot.threadName,
            requestTraceId = requestSnapshot.traceId,
            switchedThreadName = switchedWithoutPropagation.switchedThreadName,
            traceIdFromRequestContextHolderInSwitchedCoroutine = switchedWithoutPropagation.traceIdFromRequestContextHolder,
            traceIdFromExplicitArgumentInSwitchedCoroutine = traceIdFromExplicitArgument,
            traceIdFromThreadLocalContextElement = traceIdFromThreadLocalContextElement,
        )
    }

    private fun requireCurrentSnapshot(): RequestContextSnapshot {
        val attributes = RequestContextHolder.currentRequestAttributes() as? ServletRequestAttributes
            ?: error("ServletRequestAttributes가 없어 현재 요청 컨텍스트를 읽을 수 없습니다.")
        return toSnapshot(attributes)
    }

    private fun currentSnapshotOrNull(): RequestContextSnapshot? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return null
        return toSnapshot(attributes)
    }

    private fun toSnapshot(attributes: ServletRequestAttributes): RequestContextSnapshot {
        val request = attributes.request
        return RequestContextSnapshot(
            traceId = request.getHeader(TRACE_ID_HEADER),
            method = request.method,
            requestUri = request.requestURI,
            clientIp = request.remoteAddr,
            userAgent = request.getHeader(USER_AGENT_HEADER),
            threadName = Thread.currentThread().name,
        )
    }

    private data class AsyncWorkerProbe(
        val workerThreadName: String,
        val traceIdFromRequestContextHolder: String?,
        val traceIdFromExplicitArgument: String?,
    )

    private data class CoroutineThreadProbe(
        val switchedThreadName: String,
        val traceIdFromRequestContextHolder: String?,
    )

    companion object {
        private const val TRACE_ID_HEADER = "X-Request-Id"
        private const val USER_AGENT_HEADER = "User-Agent"
    }
}


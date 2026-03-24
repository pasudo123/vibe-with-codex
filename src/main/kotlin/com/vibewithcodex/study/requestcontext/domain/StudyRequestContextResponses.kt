package com.vibewithcodex.study.requestcontext.domain

/**
 * RequestContextHolder에서 추출한 핵심 요청 정보 스냅샷.
 */
data class RequestContextSnapshot(
    val traceId: String?,
    val method: String,
    val requestUri: String,
    val clientIp: String?,
    val userAgent: String?,
    val threadName: String,
)

/**
 * 현재 요청 스레드에서 RequestContextHolder 값을 확인하는 응답 모델.
 */
data class CurrentRequestContextResponse(
    val requestThreadName: String,
    val traceId: String?,
    val method: String,
    val requestUri: String,
    val clientIp: String?,
    val userAgent: String?,
)

/**
 * 비동기 스레드에서의 RequestContextHolder 가시성 비교 응답 모델.
 */
data class AsyncRequestContextResponse(
    val requestThreadName: String,
    val requestTraceId: String?,
    val workerThreadName: String,
    val traceIdFromRequestContextHolderInWorker: String?,
    val traceIdFromExplicitArgumentInWorker: String?,
)

/**
 * Coroutine 컨텍스트 전환 시 thread-bound 데이터 가시성 비교 응답 모델.
 */
data class CoroutineRequestContextResponse(
    val requestThreadName: String,
    val requestTraceId: String?,
    val switchedThreadName: String,
    val traceIdFromRequestContextHolderInSwitchedCoroutine: String?,
    val traceIdFromExplicitArgumentInSwitchedCoroutine: String?,
    val traceIdFromThreadLocalContextElement: String?,
)


package com.vibewithcodex.study.requestcontext.config

import java.util.concurrent.Executor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * RequestContextHolder 학습용 비동기 실행기 설정.
 *
 * 별도 스레드 실행 시 RequestContextHolder의 thread-bound 특성을
 * 명확히 확인할 수 있도록 전용 executor를 사용한다.
 */
@Configuration
class StudyRequestContextConfig {

    @Bean(name = [ASYNC_EXECUTOR_BEAN_NAME])
    fun studyRequestContextExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.setThreadNamePrefix("study-request-context-")
        executor.corePoolSize = 2
        executor.maxPoolSize = 2
        executor.queueCapacity = 50
        executor.initialize()
        return executor
    }

    companion object {
        const val ASYNC_EXECUTOR_BEAN_NAME = "studyRequestContextExecutor"
    }
}

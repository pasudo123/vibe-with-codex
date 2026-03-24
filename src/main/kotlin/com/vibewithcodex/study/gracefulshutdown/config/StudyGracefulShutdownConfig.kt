package com.vibewithcodex.study.gracefulshutdown.config

import java.util.concurrent.Executor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableConfigurationProperties(StudyGracefulShutdownProperties::class)
class StudyGracefulShutdownConfig {

    @Bean(name = [CALL_EXECUTOR_BEAN_NAME])
    fun gracefulShutdownCallExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.setThreadNamePrefix("study-graceful-shutdown-")
        executor.setCorePoolSize(4)
        executor.setMaxPoolSize(4)
        executor.setQueueCapacity(100)
        executor.initialize()
        return executor
    }

    companion object {
        const val CALL_EXECUTOR_BEAN_NAME = "gracefulShutdownCallExecutor"
    }
}

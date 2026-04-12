package com.vibewithcodex.study.ddd.review.infra

import com.vibewithcodex.study.ddd.review.application.ReviewEvent
import com.vibewithcodex.study.ddd.review.application.ReviewEventPublisher
import com.vibewithcodex.study.ddd.review.application.ReviewEventSubscriber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Review 이벤트를 비동기 처리하는 인메모리 퍼블리셔.
 * 학습용 구현이며, 실제 운영에서는 메시지 브로커를 사용하는 구성이 적합하다.
 */
class InMemoryAsyncReviewEventPublisher(
    private val processingDelayMillis: Long = 100,
) : ReviewEventPublisher, AutoCloseable {
    private val running = AtomicBoolean(true)
    private val queue = LinkedBlockingQueue<ReviewEvent>()
    private val subscribers = CopyOnWriteArrayList<ReviewEventSubscriber>()

    private val worker = Thread(
        {
            while (running.get() || queue.isNotEmpty()) {
                val event = queue.poll(200, TimeUnit.MILLISECONDS) ?: continue
                if (processingDelayMillis > 0) {
                    Thread.sleep(processingDelayMillis)
                }
                subscribers.forEach { it.on(event) }
            }
        },
        "study-review-event-worker",
    ).apply {
        isDaemon = true
        start()
    }

    override fun publish(event: ReviewEvent) {
        queue.offer(event)
    }

    override fun subscribe(subscriber: ReviewEventSubscriber) {
        subscribers.add(subscriber)
    }

    override fun close() {
        running.set(false)
        worker.interrupt()
        worker.join(1000)
    }
}

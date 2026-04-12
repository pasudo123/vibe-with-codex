package com.vibewithcodex.study.ddd.order.application

import org.springframework.stereotype.Component

fun interface ReviewEventSubscriber {
    fun on(event: ReviewEvent)
}

interface ReviewEventPublisher {
    fun publish(event: ReviewEvent)
    fun subscribe(subscriber: ReviewEventSubscriber)
}

@Component
object NoopReviewEventPublisher : ReviewEventPublisher {
    override fun publish(event: ReviewEvent) = Unit

    override fun subscribe(subscriber: ReviewEventSubscriber) = Unit
}

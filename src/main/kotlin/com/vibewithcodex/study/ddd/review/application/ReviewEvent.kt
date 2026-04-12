package com.vibewithcodex.study.ddd.review.application

import com.vibewithcodex.study.ddd.review.domain.ReviewId
import com.vibewithcodex.study.ddd.shared.domain.ProductId
import java.time.Instant

sealed interface ReviewEvent {
    val reviewId: ReviewId
    val productId: ProductId
    val occurredAt: Instant
}

data class ReviewWrittenEvent(
    override val reviewId: ReviewId,
    override val productId: ProductId,
    val score: Int,
    override val occurredAt: Instant = Instant.now(),
) : ReviewEvent

data class ReviewEditedEvent(
    override val reviewId: ReviewId,
    override val productId: ProductId,
    val oldScore: Int,
    val newScore: Int,
    override val occurredAt: Instant = Instant.now(),
) : ReviewEvent

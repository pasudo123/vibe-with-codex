package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.MemberId
import com.vibewithcodex.study.ddd.order.domain.ProductId
import com.vibewithcodex.study.ddd.order.domain.Review
import com.vibewithcodex.study.ddd.order.domain.ReviewId
import com.vibewithcodex.study.ddd.order.domain.ReviewScore
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class WriteReviewCommand(
    val reviewId: String,
    val productId: String,
    val reviewerId: String,
    val score: Int,
    val content: String,
)

data class EditReviewCommand(
    val reviewId: String,
    val score: Int,
    val content: String,
)

/**
 * Review 애그리거트 유스케이스를 조율하는 응용 서비스.
 */
@Service
class StudyReviewService(
    private val reviewRepository: ReviewRepository,
    private val reviewEventPublisher: ReviewEventPublisher = NoopReviewEventPublisher,
) {
    @Transactional
    fun writeReview(command: WriteReviewCommand): Review {
        val review = Review.create(
            id = ReviewId.of(command.reviewId),
            productId = ProductId.of(command.productId),
            reviewerId = MemberId.of(command.reviewerId),
            score = ReviewScore.of(command.score),
            content = command.content,
        )
        return review.also {
            reviewRepository.save(it)
            reviewEventPublisher.publish(
                ReviewWrittenEvent(
                    reviewId = it.id,
                    productId = it.productId,
                    score = it.score.value,
                    occurredAt = Instant.now(),
                ),
            )
        }
    }

    @Transactional
    fun editReview(command: EditReviewCommand): Review {
        val reviewId = ReviewId.of(command.reviewId)
        val review = reviewRepository.findById(reviewId)
            ?: throw EntityNotFoundException(entity = "리뷰", id = command.reviewId)
        val oldScore = review.score.value
        review.edit(newScore = ReviewScore.of(command.score), newContent = command.content)
        return review.also {
            reviewRepository.save(it)
            reviewEventPublisher.publish(
                ReviewEditedEvent(
                    reviewId = it.id,
                    productId = it.productId,
                    oldScore = oldScore,
                    newScore = it.score.value,
                    occurredAt = Instant.now(),
                ),
            )
        }
    }
}

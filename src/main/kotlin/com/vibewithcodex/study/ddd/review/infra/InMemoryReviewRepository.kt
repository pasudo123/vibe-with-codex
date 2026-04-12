package com.vibewithcodex.study.ddd.review.infra

import com.vibewithcodex.study.ddd.review.application.ReviewRepository
import com.vibewithcodex.study.ddd.review.domain.Review
import com.vibewithcodex.study.ddd.review.domain.ReviewId
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

@Repository
class InMemoryReviewRepository : ReviewRepository {
    private val storage = ConcurrentHashMap<ReviewId, Review>()

    override fun save(review: Review) {
        storage[review.id] = review
    }

    override fun findById(reviewId: ReviewId): Review? = storage[reviewId]
}

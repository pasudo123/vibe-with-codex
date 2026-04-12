package com.vibewithcodex.study.ddd.review.application

import com.vibewithcodex.study.ddd.review.domain.Review
import com.vibewithcodex.study.ddd.review.domain.ReviewId

interface ReviewRepository {
    fun save(review: Review)
    fun findById(reviewId: ReviewId): Review?
}

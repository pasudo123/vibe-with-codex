package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Review
import com.vibewithcodex.study.ddd.order.domain.ReviewId

interface ReviewRepository {
    fun save(review: Review)
    fun findById(reviewId: ReviewId): Review?
}

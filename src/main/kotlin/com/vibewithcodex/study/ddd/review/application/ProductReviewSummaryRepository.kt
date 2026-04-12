package com.vibewithcodex.study.ddd.review.application

import com.vibewithcodex.study.ddd.review.domain.ProductReviewSummary
import com.vibewithcodex.study.ddd.shared.domain.ProductId

interface ProductReviewSummaryRepository {
    fun save(summary: ProductReviewSummary)
    fun findByProductId(productId: ProductId): ProductReviewSummary?
}

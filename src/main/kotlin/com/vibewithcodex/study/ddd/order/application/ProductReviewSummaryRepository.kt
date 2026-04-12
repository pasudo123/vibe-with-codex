package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.ProductId
import com.vibewithcodex.study.ddd.order.domain.ProductReviewSummary

interface ProductReviewSummaryRepository {
    fun save(summary: ProductReviewSummary)
    fun findByProductId(productId: ProductId): ProductReviewSummary?
}

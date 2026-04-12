package com.vibewithcodex.study.ddd.review.infra

import com.vibewithcodex.study.ddd.review.application.ProductReviewSummaryRepository
import com.vibewithcodex.study.ddd.review.domain.ProductReviewSummary
import com.vibewithcodex.study.ddd.shared.domain.ProductId
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

@Repository
class InMemoryProductReviewSummaryRepository : ProductReviewSummaryRepository {
    private val storage = ConcurrentHashMap<ProductId, ProductReviewSummary>()

    override fun save(summary: ProductReviewSummary) {
        storage[summary.productId] = summary
    }

    override fun findByProductId(productId: ProductId): ProductReviewSummary? = storage[productId]
}

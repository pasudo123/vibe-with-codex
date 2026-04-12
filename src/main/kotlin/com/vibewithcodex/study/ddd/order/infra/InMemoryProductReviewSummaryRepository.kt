package com.vibewithcodex.study.ddd.order.infra

import com.vibewithcodex.study.ddd.order.application.ProductReviewSummaryRepository
import com.vibewithcodex.study.ddd.order.domain.ProductId
import com.vibewithcodex.study.ddd.order.domain.ProductReviewSummary
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

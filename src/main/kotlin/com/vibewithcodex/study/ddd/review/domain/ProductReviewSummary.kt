package com.vibewithcodex.study.ddd.review.domain

import com.vibewithcodex.study.ddd.shared.domain.ProductId

/**
 * 상품 리뷰 요약 리드모델.
 * 리뷰 애그리거트 변경 이벤트를 비동기 반영해 eventual consistency로 관리한다.
 */
data class ProductReviewSummary private constructor(
    val productId: ProductId,
    val reviewCount: Int,
    private val totalScore: Int,
) {
    val averageScore: Double
        get() = if (reviewCount == 0) 0.0 else totalScore.toDouble() / reviewCount

    fun applyReviewWritten(score: Int): ProductReviewSummary =
        ProductReviewSummary(
            productId = productId,
            reviewCount = reviewCount + 1,
            totalScore = totalScore + score,
        )

    fun applyReviewEdited(oldScore: Int, newScore: Int): ProductReviewSummary =
        ProductReviewSummary(
            productId = productId,
            reviewCount = reviewCount,
            totalScore = totalScore - oldScore + newScore,
        )

    companion object {
        fun empty(productId: ProductId): ProductReviewSummary =
            ProductReviewSummary(
                productId = productId,
                reviewCount = 0,
                totalScore = 0,
            )
    }
}

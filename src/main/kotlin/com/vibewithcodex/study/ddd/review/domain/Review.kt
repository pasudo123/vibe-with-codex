package com.vibewithcodex.study.ddd.review.domain

import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException
import com.vibewithcodex.study.ddd.shared.domain.MemberId
import com.vibewithcodex.study.ddd.shared.domain.ProductId
import java.time.Instant

/**
 * 리뷰 애그리거트 루트.
 * Product를 직접 참조하지 않고 식별자(productId)로만 연결해 경계를 분리한다.
 */
class Review private constructor(
    val id: ReviewId,
    val productId: ProductId,
    val reviewerId: MemberId,
    score: ReviewScore,
    content: String,
    val createdAt: Instant,
) {
    var score: ReviewScore = score
        private set

    var content: String = content
        private set

    fun edit(newScore: ReviewScore, newContent: String) {
        if (newContent.isBlank()) {
            throw InvalidOrderException("리뷰 내용은 비어 있을 수 없습니다.")
        }
        score = newScore
        content = newContent
    }

    companion object {
        fun create(
            id: ReviewId,
            productId: ProductId,
            reviewerId: MemberId,
            score: ReviewScore,
            content: String,
            createdAt: Instant = Instant.now(),
        ): Review {
            if (content.isBlank()) {
                throw InvalidOrderException("리뷰 내용은 비어 있을 수 없습니다.")
            }
            return Review(
                id = id,
                productId = productId,
                reviewerId = reviewerId,
                score = score,
                content = content,
                createdAt = createdAt,
            )
        }
    }
}

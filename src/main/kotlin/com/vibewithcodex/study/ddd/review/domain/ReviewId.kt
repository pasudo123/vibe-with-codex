package com.vibewithcodex.study.ddd.review.domain

import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException

/**
 * 리뷰 식별자 밸류.
 */
@JvmInline
value class ReviewId private constructor(
    val value: String,
) {
    companion object {
        fun of(value: String): ReviewId {
            if (value.isBlank()) {
                throw InvalidOrderException("리뷰 아이디는 비어 있을 수 없습니다.")
            }
            return ReviewId(value)
        }
    }
}

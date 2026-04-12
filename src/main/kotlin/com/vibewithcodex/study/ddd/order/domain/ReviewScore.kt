package com.vibewithcodex.study.ddd.order.domain

/**
 * 리뷰 점수 밸류(1~5).
 */
@JvmInline
value class ReviewScore private constructor(
    val value: Int,
) {
    companion object {
        fun of(value: Int): ReviewScore {
            if (value !in 1..5) {
                throw InvalidOrderException("리뷰 점수는 1점에서 5점 사이여야 합니다.")
            }
            return ReviewScore(value)
        }
    }
}

package com.vibewithcodex.study.ddd.order.domain

/**
 * 카테고리 식별자 밸류.
 */
@JvmInline
value class CategoryId private constructor(
    val value: String,
) {
    companion object {
        fun of(value: String): CategoryId {
            if (value.isBlank()) {
                throw InvalidOrderException("카테고리 아이디는 비어 있을 수 없습니다.")
            }
            return CategoryId(value)
        }
    }
}

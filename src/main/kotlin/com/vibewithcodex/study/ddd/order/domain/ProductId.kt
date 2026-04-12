package com.vibewithcodex.study.ddd.order.domain

/**
 * 상품 식별자 밸류.
 */
@JvmInline
value class ProductId private constructor(
    val value: String,
) {
    companion object {
        fun of(value: String): ProductId {
            if (value.isBlank()) {
                throw InvalidOrderException("상품 아이디는 비어 있을 수 없습니다.")
            }
            return ProductId(value)
        }
    }
}

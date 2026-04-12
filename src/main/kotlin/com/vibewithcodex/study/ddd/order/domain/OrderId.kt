package com.vibewithcodex.study.ddd.order.domain

/**
 * 주문 엔티티 식별자 밸류.
 * 엔티티의 동일성은 이 값으로 판단한다.
 */
@JvmInline
value class OrderId private constructor(
    val value: String,
) {
    companion object {
        fun of(value: String): OrderId {
            if (value.isBlank()) {
                throw InvalidOrderException("주문 아이디는 비어 있을 수 없습니다.")
            }
            return OrderId(value)
        }
    }
}

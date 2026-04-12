package com.vibewithcodex.study.ddd.ordering.domain

import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException
import com.vibewithcodex.study.ddd.shared.domain.Money
import com.vibewithcodex.study.ddd.shared.domain.ProductId

/**
 * 주문 항목 밸류 객체.
 * 수량/상품/단가의 최소 유효성은 생성 시점에 검증한다.
 */
data class OrderLine(
    val productId: ProductId,
    val unitPrice: Money,
    val quantity: Int,
) {
    init {
        if (quantity <= 0) {
            throw InvalidOrderException("주문 항목 수량은 0보다 커야 합니다.")
        }
    }

    fun totalPrice(): Money = unitPrice.multiply(quantity)
}

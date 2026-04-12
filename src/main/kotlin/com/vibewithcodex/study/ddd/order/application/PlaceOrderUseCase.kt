package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Order

/**
 * 주문 생성 유스케이스 인터페이스.
 */
interface PlaceOrderUseCase {
    fun placeOrder(command: PlaceOrderCommand): Order
}

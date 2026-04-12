package com.vibewithcodex.study.ddd.ordering.application

import com.vibewithcodex.study.ddd.ordering.domain.Order
import com.vibewithcodex.study.ddd.shared.domain.OrderId

/**
 * 주문 저장소 추상화.
 * 응용 계층은 이 인터페이스에 의존하고, 인프라 계층이 구현한다.
 */
interface OrderRepository {
    fun save(order: Order)
    fun findById(orderId: OrderId): Order?
}

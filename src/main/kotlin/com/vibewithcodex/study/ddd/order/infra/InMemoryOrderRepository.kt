package com.vibewithcodex.study.ddd.order.infra

import com.vibewithcodex.study.ddd.order.application.OrderRepository
import com.vibewithcodex.study.ddd.order.domain.Order
import com.vibewithcodex.study.ddd.order.domain.OrderId
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

/**
 * 학습용 인메모리 저장소 구현체.
 * 저장 기술 구현은 인프라 계층이 담당한다.
 */
@Repository
class InMemoryOrderRepository : OrderRepository {
    private val storage = ConcurrentHashMap<OrderId, Order>()

    override fun save(order: Order) {
        storage[order.id] = order
    }

    override fun findById(orderId: OrderId): Order? = storage[orderId]
}

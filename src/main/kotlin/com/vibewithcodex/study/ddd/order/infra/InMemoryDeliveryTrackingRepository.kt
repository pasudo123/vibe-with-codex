package com.vibewithcodex.study.ddd.order.infra

import com.vibewithcodex.study.ddd.order.application.DeliveryTrackingRepository
import com.vibewithcodex.study.ddd.order.domain.DeliveryTracking
import com.vibewithcodex.study.ddd.order.domain.OrderId
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

@Repository
class InMemoryDeliveryTrackingRepository : DeliveryTrackingRepository {
    private val storage = ConcurrentHashMap<OrderId, DeliveryTracking>()

    override fun save(deliveryTracking: DeliveryTracking) {
        storage[deliveryTracking.orderId] = deliveryTracking
    }

    override fun findByOrderId(orderId: OrderId): DeliveryTracking? = storage[orderId]
}

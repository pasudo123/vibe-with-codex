package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.DeliveryTracking
import com.vibewithcodex.study.ddd.order.domain.OrderId

interface DeliveryTrackingRepository {
    fun save(deliveryTracking: DeliveryTracking)
    fun findByOrderId(orderId: OrderId): DeliveryTracking?
}

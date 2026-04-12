package com.vibewithcodex.study.ddd.shipping.application

import com.vibewithcodex.study.ddd.shipping.domain.DeliveryTracking
import com.vibewithcodex.study.ddd.shared.domain.OrderId

interface DeliveryTrackingRepository {
    fun save(deliveryTracking: DeliveryTracking)
    fun findByOrderId(orderId: OrderId): DeliveryTracking?
}

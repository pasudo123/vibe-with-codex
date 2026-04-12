package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.DeliveryTracking
import com.vibewithcodex.study.ddd.order.domain.OrderId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateDeliveryTrackingCommand(
    val orderId: String,
    val trackingNumber: String,
    val carrier: String,
)

/**
 * DeliveryTracking 애그리거트 유스케이스를 조율하는 응용 서비스.
 */
@Service
class StudyDeliveryTrackingService(
    private val deliveryTrackingRepository: DeliveryTrackingRepository,
) {
    @Transactional
    fun createDeliveryTracking(command: CreateDeliveryTrackingCommand): DeliveryTracking {
        val deliveryTracking = DeliveryTracking.create(
            orderId = OrderId.of(command.orderId),
            trackingNumber = command.trackingNumber,
            carrier = command.carrier,
        )
        return deliveryTracking.also(deliveryTrackingRepository::save)
    }

    @Transactional
    fun markInTransit(orderId: String): DeliveryTracking {
        val id = OrderId.of(orderId)
        val deliveryTracking = deliveryTrackingRepository.findByOrderId(id)
            ?: throw EntityNotFoundException(entity = "배송 추적", id = orderId)
        deliveryTracking.markInTransit()
        return deliveryTracking.also(deliveryTrackingRepository::save)
    }

    @Transactional
    fun markDelivered(orderId: String): DeliveryTracking {
        val id = OrderId.of(orderId)
        val deliveryTracking = deliveryTrackingRepository.findByOrderId(id)
            ?: throw EntityNotFoundException(entity = "배송 추적", id = orderId)
        deliveryTracking.markDelivered()
        return deliveryTracking.also(deliveryTrackingRepository::save)
    }
}

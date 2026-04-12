package com.vibewithcodex.study.ddd.order.domain

import java.time.Instant

/**
 * 배송 추적 엔티티.
 * 주문 식별자를 기준으로 배송 상태를 단계적으로 전이시킨다.
 */
class DeliveryTracking private constructor(
    val orderId: OrderId,
    val trackingNumber: String,
    val carrier: String,
    status: DeliveryStatus,
    startedAt: Instant?,
    deliveredAt: Instant?,
) {
    var status: DeliveryStatus = status
        private set

    var startedAt: Instant? = startedAt
        private set

    var deliveredAt: Instant? = deliveredAt
        private set

    fun markInTransit(at: Instant = Instant.now()) {
        if (status != DeliveryStatus.READY) {
            throw InvalidOrderStateException("배송은 배송 준비 상태에서만 시작할 수 있습니다.")
        }
        status = DeliveryStatus.IN_TRANSIT
        startedAt = at
    }

    fun markDelivered(at: Instant = Instant.now()) {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw InvalidOrderStateException("배송 완료는 배송 중 상태에서만 처리할 수 있습니다.")
        }
        status = DeliveryStatus.DELIVERED
        deliveredAt = at
    }

    companion object {
        fun create(
            orderId: OrderId,
            trackingNumber: String,
            carrier: String,
        ): DeliveryTracking {
            if (trackingNumber.isBlank()) {
                throw InvalidOrderException("운송장 번호는 비어 있을 수 없습니다.")
            }
            if (carrier.isBlank()) {
                throw InvalidOrderException("택배사는 비어 있을 수 없습니다.")
            }
            return DeliveryTracking(
                orderId = orderId,
                trackingNumber = trackingNumber,
                carrier = carrier,
                status = DeliveryStatus.READY,
                startedAt = null,
                deliveredAt = null,
            )
        }
    }
}

package com.vibewithcodex.study.ddd.ordering.domain

import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException
import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderStateException
import com.vibewithcodex.study.ddd.shared.domain.Money
import com.vibewithcodex.study.ddd.shared.domain.OrderId
import java.time.Instant

/**
 * 주문 애그리거트 루트(엔티티).
 *
 * - 식별자(`OrderId`)로 동일성을 판단한다.
 * - 생성은 `create` 팩토리를 통해서만 허용해, 생성 시점 유효 상태를 보장한다.
 * - 상태 변경은 도메인 메서드로만 노출해 불변식이 깨지지 않게 한다.
 */
class Order private constructor(
    val id: OrderId,
    val orderer: Orderer,
    val orderLines: List<OrderLine>,
    shippingInfo: ShippingInfo,
    val totalAmount: Money,
    val createdAt: Instant,
) {
    var shippingInfo: ShippingInfo = shippingInfo
        private set

    var status: OrderStatus = OrderStatus.CREATED
        private set

    var canceledAt: Instant? = null
        private set

    /**
     * 배송지 변경 규칙:
     * 취소된 주문은 수정할 수 없다.
     */
    fun changeShippingInfo(newShippingInfo: ShippingInfo) {
        ensureNotCanceled()
        shippingInfo = newShippingInfo
    }

    /**
     * 취소 규칙:
     * 이미 취소된 주문은 다시 취소/수정할 수 없다.
     */
    fun cancel(canceledTime: Instant = Instant.now()) {
        ensureNotCanceled()
        status = OrderStatus.CANCELED
        canceledAt = canceledTime
    }

    private fun ensureNotCanceled() {
        if (status == OrderStatus.CANCELED) {
            throw InvalidOrderStateException("취소된 주문은 수정할 수 없습니다.")
        }
    }

    companion object {
        /**
         * 주문 생성 팩토리.
         *
         * 생성 이후 setter로 필수값을 채우지 않도록, 필요한 값과 검증을 이 경계에 모은다.
         */
        fun create(
            id: OrderId,
            orderer: Orderer,
            orderLines: List<OrderLine>,
            shippingInfo: ShippingInfo,
            createdAt: Instant = Instant.now(),
        ): Order {
            if (orderLines.isEmpty()) {
                throw InvalidOrderException("주문 항목은 최소 1개 이상이어야 합니다.")
            }

            val immutableLines = orderLines.toList()
            val total = immutableLines.fold(Money.zero()) { acc, line -> acc.plus(line.totalPrice()) }

            return Order(
                id = id,
                orderer = orderer,
                orderLines = immutableLines,
                shippingInfo = shippingInfo,
                totalAmount = total,
                createdAt = createdAt,
            )
        }
    }
}

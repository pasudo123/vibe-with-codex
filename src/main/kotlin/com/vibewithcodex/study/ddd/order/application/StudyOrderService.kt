package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Order
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 애플리케이션 서비스.
 *
 * 도메인 규칙을 직접 구현하지 않고, 유스케이스 흐름을 조합하고
 * 검증/상태변경은 도메인 모델에 위임한다.
 */
@Service
class StudyOrderService(
    private val orderRepository: OrderRepository,
) : PlaceOrderUseCase, ChangeShippingInfoUseCase {
    /**
     * 주문 생성 유스케이스.
     * 생성 시점 검증은 `Order.create`와 밸류 객체 생성자에서 수행된다.
     */
    @Transactional
    override fun placeOrder(command: PlaceOrderCommand): Order =
        command.toDomainOrder().also(orderRepository::save)

    /**
     * 배송 정보 변경 유스케이스.
     * 취소 여부 같은 불변식은 `Order.changeShippingInfo`가 보호한다.
     */
    @Transactional
    override fun changeShippingInfo(command: ChangeShippingInfoCommand): Order {
        val orderId = command.orderId.toDomainOrderId()
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException(orderId)

        order.changeShippingInfo(command.toDomainShippingInfo())
        return order.also(orderRepository::save)
    }
}

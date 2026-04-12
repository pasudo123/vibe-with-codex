package com.vibewithcodex.study.ddd.order.api

import com.vibewithcodex.study.ddd.order.application.ChangeShippingInfoCommand
import com.vibewithcodex.study.ddd.order.application.ChangeShippingInfoUseCase
import com.vibewithcodex.study.ddd.order.application.PlaceOrderCommand
import com.vibewithcodex.study.ddd.order.application.PlaceOrderUseCase
import com.vibewithcodex.study.ddd.order.application.ShippingInfoCommand
import com.vibewithcodex.study.ddd.order.domain.Order
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 주문 바운디드 컨텍스트의 학습용 API.
 *
 * 바운디드 컨텍스트를 도메인 모델만으로 한정하지 않고,
 * 외부 요청을 받는 API 레이어도 같은 의미 경계 안에서 함께 다룬다.
 */
@RestController
@RequestMapping("/study/ddd/order")
class StudyOrderController(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val changeShippingInfoUseCase: ChangeShippingInfoUseCase,
) {
    @PostMapping("/orders")
    fun placeOrder(@RequestBody command: PlaceOrderCommand): OrderSummaryResponse =
        placeOrderUseCase.placeOrder(command).toSummaryResponse()

    @PutMapping("/orders/{orderId}/shipping-info")
    fun changeShippingInfo(
        @PathVariable orderId: String,
        @RequestBody shippingInfo: ShippingInfoCommand,
    ): OrderSummaryResponse =
        changeShippingInfoUseCase.changeShippingInfo(
            ChangeShippingInfoCommand(
                orderId = orderId,
                shippingInfo = shippingInfo,
            ),
        ).toSummaryResponse()
}

data class OrderSummaryResponse(
    val orderId: String,
    val status: String,
    val totalAmount: Long,
    val orderLineCount: Int,
    val ordererName: String,
    val receiverName: String,
    val shippingZipCode: String,
)

private fun Order.toSummaryResponse(): OrderSummaryResponse =
    OrderSummaryResponse(
        orderId = id.value,
        status = status.name,
        totalAmount = totalAmount.amount,
        orderLineCount = orderLines.size,
        ordererName = orderer.name,
        receiverName = shippingInfo.receiver.name,
        shippingZipCode = shippingInfo.address.zipCode,
    )

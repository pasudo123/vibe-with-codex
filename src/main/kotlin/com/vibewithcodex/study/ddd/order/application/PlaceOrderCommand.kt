package com.vibewithcodex.study.ddd.order.application

data class PlaceOrderCommand(
    val orderId: String,
    val orderer: OrdererCommand,
    val shippingInfo: ShippingInfoCommand,
    val orderLines: List<OrderLineCommand>,
)

data class OrdererCommand(
    val memberId: String,
    val name: String,
)

data class ReceiverCommand(
    val name: String,
    val phoneNumber: String,
)

data class AddressCommand(
    val zipCode: String,
    val address1: String,
    val address2: String = "",
)

data class ShippingInfoCommand(
    val receiver: ReceiverCommand,
    val address: AddressCommand,
    val message: String = "",
)

data class OrderLineCommand(
    val productId: String,
    val unitPrice: Long,
    val quantity: Int,
)

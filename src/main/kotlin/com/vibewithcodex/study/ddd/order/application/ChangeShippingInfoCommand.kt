package com.vibewithcodex.study.ddd.order.application

data class ChangeShippingInfoCommand(
    val orderId: String,
    val shippingInfo: ShippingInfoCommand,
)

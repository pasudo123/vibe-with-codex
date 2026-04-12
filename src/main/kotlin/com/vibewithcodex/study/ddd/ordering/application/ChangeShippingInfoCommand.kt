package com.vibewithcodex.study.ddd.ordering.application

data class ChangeShippingInfoCommand(
    val orderId: String,
    val shippingInfo: ShippingInfoCommand,
)

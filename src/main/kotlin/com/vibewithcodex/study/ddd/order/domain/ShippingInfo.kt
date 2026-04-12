package com.vibewithcodex.study.ddd.order.domain

/**
 * 배송 정보를 묶는 밸류 객체.
 * 수령인/주소/배송 메모를 하나의 일관성 단위로 다룬다.
 */
data class ShippingInfo(
    val receiver: Receiver,
    val address: Address,
    val message: String = "",
)

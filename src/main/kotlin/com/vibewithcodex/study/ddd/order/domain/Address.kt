package com.vibewithcodex.study.ddd.order.domain

/**
 * 주소를 표현하는 밸류 객체.
 */
data class Address(
    val zipCode: String,
    val address1: String,
    val address2: String = "",
) {
    init {
        if (zipCode.isBlank()) {
            throw InvalidOrderException("우편번호는 비어 있을 수 없습니다.")
        }
        if (address1.isBlank()) {
            throw InvalidOrderException("기본 주소는 비어 있을 수 없습니다.")
        }
    }
}

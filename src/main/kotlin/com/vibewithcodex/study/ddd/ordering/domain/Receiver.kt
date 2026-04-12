package com.vibewithcodex.study.ddd.ordering.domain

import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException

/**
 * 수령인 정보를 표현하는 밸류 객체.
 */
data class Receiver(
    val name: String,
    val phoneNumber: String,
) {
    init {
        if (name.isBlank()) {
            throw InvalidOrderException("수령인 이름은 비어 있을 수 없습니다.")
        }
        if (phoneNumber.isBlank()) {
            throw InvalidOrderException("수령인 연락처는 비어 있을 수 없습니다.")
        }
    }
}

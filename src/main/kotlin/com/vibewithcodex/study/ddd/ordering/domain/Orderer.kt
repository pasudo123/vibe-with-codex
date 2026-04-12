package com.vibewithcodex.study.ddd.ordering.domain

import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException
import com.vibewithcodex.study.ddd.shared.domain.MemberId

/**
 * 주문자 정보를 표현하는 밸류 객체.
 * 엔티티가 아닌 값 모델이므로 값 동등성으로 비교된다.
 */
data class Orderer(
    val memberId: MemberId,
    val name: String,
) {
    init {
        if (name.isBlank()) {
            throw InvalidOrderException("주문자 이름은 비어 있을 수 없습니다.")
        }
    }
}

package com.vibewithcodex.study.ddd.order.domain

import java.time.Instant

/**
 * 결제 승인 정보를 표현하는 밸류 객체.
 */
data class PaymentInfo(
    val method: PaymentMethod,
    val approvedAmount: Money,
    val transactionId: String,
    val approvedAt: Instant,
) {
    init {
        if (approvedAmount.amount <= 0) {
            throw InvalidOrderException("승인 금액은 0보다 커야 합니다.")
        }
        if (transactionId.isBlank()) {
            throw InvalidOrderException("거래 아이디는 비어 있을 수 없습니다.")
        }
    }
}

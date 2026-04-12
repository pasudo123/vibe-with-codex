package com.vibewithcodex.study.ddd.shared.domain

/**
 * 금액 밸류 객체.
 *
 * 값 그 자체가 동일성 기준이며, 생성 시점에 음수 금액을 차단한다.
 */
data class Money private constructor(
    val amount: Long,
) {
    fun plus(other: Money): Money = of(amount + other.amount)

    fun multiply(multiplier: Int): Money {
        if (multiplier <= 0) {
            throw InvalidOrderException("수량 배수는 0보다 커야 합니다.")
        }
        return of(amount * multiplier)
    }

    companion object {
        fun of(amount: Long): Money {
            if (amount < 0) {
                throw InvalidOrderException("금액은 0 이상이어야 합니다.")
            }
            return Money(amount)
        }

        fun zero(): Money = Money(0)
    }
}

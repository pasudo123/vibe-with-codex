package com.vibewithcodex.study.ddd.shared.domain

/**
 * 주문자 식별자 밸류.
 */
@JvmInline
value class MemberId private constructor(
    val value: String,
) {
    companion object {
        fun of(value: String): MemberId {
            if (value.isBlank()) {
                throw InvalidOrderException("회원 아이디는 비어 있을 수 없습니다.")
            }
            return MemberId(value)
        }
    }
}

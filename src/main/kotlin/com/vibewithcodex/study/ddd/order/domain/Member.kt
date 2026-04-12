package com.vibewithcodex.study.ddd.order.domain

/**
 * 회원 엔티티.
 */
class Member private constructor(
    val id: MemberId,
    name: String,
    grade: MemberGrade,
) {
    var name: String = name
        private set

    var grade: MemberGrade = grade
        private set

    fun changeName(newName: String) {
        if (newName.isBlank()) {
            throw InvalidOrderException("회원 이름은 비어 있을 수 없습니다.")
        }
        name = newName
    }

    fun changeGrade(newGrade: MemberGrade) {
        grade = newGrade
    }

    companion object {
        fun create(
            id: MemberId,
            name: String,
            grade: MemberGrade = MemberGrade.BRONZE,
        ): Member {
            if (name.isBlank()) {
                throw InvalidOrderException("회원 이름은 비어 있을 수 없습니다.")
            }
            return Member(id = id, name = name, grade = grade)
        }
    }
}

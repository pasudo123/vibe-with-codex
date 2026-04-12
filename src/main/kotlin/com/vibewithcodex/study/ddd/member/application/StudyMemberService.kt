package com.vibewithcodex.study.ddd.member.application

import com.vibewithcodex.study.ddd.member.domain.Member
import com.vibewithcodex.study.ddd.member.domain.MemberGrade
import com.vibewithcodex.study.ddd.shared.application.EntityNotFoundException
import com.vibewithcodex.study.ddd.shared.domain.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RegisterMemberCommand(
    val memberId: String,
    val name: String,
    val grade: MemberGrade = MemberGrade.BRONZE,
)

/**
 * Member 애그리거트 유스케이스를 조율하는 응용 서비스.
 */
@Service
class StudyMemberService(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun registerMember(command: RegisterMemberCommand): Member {
        val member = Member.create(
            id = MemberId.of(command.memberId),
            name = command.name,
            grade = command.grade,
        )
        return member.also(memberRepository::save)
    }

    @Transactional
    fun changeMemberGrade(memberId: String, grade: MemberGrade): Member {
        val id = MemberId.of(memberId)
        val member = memberRepository.findById(id)
            ?: throw EntityNotFoundException(entity = "회원", id = memberId)
        member.changeGrade(grade)
        return member.also(memberRepository::save)
    }
}

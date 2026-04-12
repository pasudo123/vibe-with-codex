package com.vibewithcodex.study.ddd.member.application

import com.vibewithcodex.study.ddd.member.domain.Member
import com.vibewithcodex.study.ddd.shared.domain.MemberId

interface MemberRepository {
    fun save(member: Member)
    fun findById(memberId: MemberId): Member?
}

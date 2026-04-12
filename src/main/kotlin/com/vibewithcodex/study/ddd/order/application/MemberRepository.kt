package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Member
import com.vibewithcodex.study.ddd.order.domain.MemberId

interface MemberRepository {
    fun save(member: Member)
    fun findById(memberId: MemberId): Member?
}

package com.vibewithcodex.study.ddd.order.infra

import com.vibewithcodex.study.ddd.order.application.MemberRepository
import com.vibewithcodex.study.ddd.order.domain.Member
import com.vibewithcodex.study.ddd.order.domain.MemberId
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

@Repository
class InMemoryMemberRepository : MemberRepository {
    private val storage = ConcurrentHashMap<MemberId, Member>()

    override fun save(member: Member) {
        storage[member.id] = member
    }

    override fun findById(memberId: MemberId): Member? = storage[memberId]
}

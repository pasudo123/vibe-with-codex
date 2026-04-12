package com.vibewithcodex.study.ddd.member.infra

import com.vibewithcodex.study.ddd.member.application.MemberRepository
import com.vibewithcodex.study.ddd.member.domain.Member
import com.vibewithcodex.study.ddd.shared.domain.MemberId
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

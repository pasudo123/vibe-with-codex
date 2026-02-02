package com.vibewithcodex.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SlugifierDescribeSpecTest : DescribeSpec({
    val slugifier = Slugifier()

    describe("DescribeSpec: BDD describe/it 스타일") {
        it("소문자로 변환하고 하이픈으로 연결한다") {
            slugifier.slugify("Hello World") shouldBe "hello-world"
        }

        it("연속 구분자는 하나의 하이픈으로 정규화된다") {
            slugifier.slugify("Already--Slug") shouldBe "already-slug"
        }
    }
})

package com.vibewithcodex.core

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class SlugifierShouldSpecTest : ShouldSpec({
    val slugifier = Slugifier()

    context("ShouldSpec: 계층형 문장 스타일") {
        should("입력 문자열을 슬러그로 변환한다") {
            slugifier.slugify("Kotlin_101!!") shouldBe "kotlin-101"
        }

        should("기호만 있으면 빈 문자열이 된다") {
            slugifier.slugify("###") shouldBe ""
        }
    }
})

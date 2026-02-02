package com.vibewithcodex.core

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SlugifierBehaviorSpecTest : BehaviorSpec({
    val slugifier = Slugifier()

    given("BehaviorSpec: given/when/then 흐름") {
        `when`("공백이 많은 입력을 변환하면") {
            val result = slugifier.slugify("  Multiple   Spaces  ")

            then("공백은 하나의 하이픈으로 변환된다") {
                result shouldBe "multiple-spaces"
            }
        }
    }
})

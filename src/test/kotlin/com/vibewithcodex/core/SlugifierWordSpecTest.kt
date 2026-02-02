package com.vibewithcodex.core

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

class SlugifierWordSpecTest : WordSpec({
    val slugifier = Slugifier()

    // WordSpec: "should" 문장을 자연어처럼 읽히게 만들어 의도를 명확히 드러낸다.
    "slugify는" should {
        // should: 특정 행위에 대한 기대 결과를 문장 형태로 표현한다.
        "대문자를 소문자로 바꾸고 공백을 하이픈으로 바꾼다" {
            val result = slugifier.slugify("Hello World")
            result shouldBe "hello-world"
        }

        // should: 잡다한 구분자가 하나의 하이픈으로 합쳐지는지 검증한다.
        "연속 구분자를 하나의 하이픈으로 정규화한다" {
            val result = slugifier.slugify("Already--Slug")
            result shouldBe "already-slug"
        }
    }
})

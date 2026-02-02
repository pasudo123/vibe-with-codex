package com.vibewithcodex.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SlugifierStringSpecTest : StringSpec({
    val slugifier = Slugifier()

    "StringSpec: 문자열 자체가 테스트 이름이 된다" {
        slugifier.slugify("Hello World") shouldBe "hello-world"
    }

    "StringSpec: 공백 정리와 하이픈 처리" {
        slugifier.slugify("  Multiple   Spaces  ") shouldBe "multiple-spaces"
    }
})

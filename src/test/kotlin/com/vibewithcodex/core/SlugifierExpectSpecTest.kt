package com.vibewithcodex.core

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class SlugifierExpectSpecTest : ExpectSpec({
    val slugifier = Slugifier()

    // expect: 기대 결과를 강조하는 스타일. "이게 되어야 한다"는 의미를 직관적으로 표현한다.
    expect("기본 동작: 공백은 하이픈으로, 대문자는 소문자로") {
        val result = slugifier.slugify("Hello World")
        result shouldBe "hello-world"
    }

    // context/expect: 관련 테스트를 의미 있는 묶음으로 정리할 때 사용한다.
    context("입력 전처리 규칙") {
        // expect: 잡다한 구분자(공백/기호)가 하나의 하이픈으로 정규화되는지 확인한다.
        expect("연속 구분자는 하나의 하이픈으로 합쳐진다") {
            val result = slugifier.slugify("  Multiple   Spaces  ")
            result shouldBe "multiple-spaces"
        }

        // expect: 입력이 기호만 있을 때 빈 문자열이 되는지 확인한다.
        expect("기호만 있으면 빈 문자열이 된다") {
            val result = slugifier.slugify("###")
            result shouldBe ""
        }
    }
})

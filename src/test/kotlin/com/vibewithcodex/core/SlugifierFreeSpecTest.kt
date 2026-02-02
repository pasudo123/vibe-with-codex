package com.vibewithcodex.core

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class SlugifierFreeSpecTest : FreeSpec({
    val slugifier = Slugifier()

    // FreeSpec: 자유로운 중첩 구조로 시나리오를 표현한다.
    "slugify" - {
        // "-" 구문: 상위 설명 아래에 하위 테스트를 계층적으로 배치한다.
        "기본 변환" - {
            // 이 테스트는 공백과 대문자 처리 규칙을 한 번에 확인한다.
            "공백은 하이픈으로, 대문자는 소문자로 변환된다" {
                val result = slugifier.slugify("Hello World")
                result shouldBe "hello-world"
            }
        }

        "특수 케이스" - {
            // 이 테스트는 입력이 기호만 있을 때의 예외적인 결과를 검증한다.
            "기호만 있으면 빈 문자열이 된다" {
                val result = slugifier.slugify("###")
                result shouldBe ""
            }
        }
    }
})

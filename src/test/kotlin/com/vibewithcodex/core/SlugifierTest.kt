package com.vibewithcodex.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.forAll
import io.kotest.matchers.regex.shouldMatch
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.checkAll
import io.kotest.property.arbitrary.stringPattern
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import kotlin.time.Duration.Companion.seconds

class SlugifierTest : FunSpec({
    val slugifier = Slugifier()

    // withData: 입력1/기대값 테이블을 만들어 동일한 검증을 반복 실행한다.
    context("데이터 기반 예제") {
        withData(
            nameFn = { (input, expected) -> "\"$input\" -> \"$expected\"" },
            "Hello World" to "hello-world",
            "  Multiple   Spaces  " to "multiple-spaces",
            "Already--Slug" to "already-slug",
            "Kotlin_101!!" to "kotlin-101",
            "###" to "",
        ) { (input, expected) ->
            slugifier.slugify(input) shouldBe expected
        }
    }

    // property 테스트: 랜덤 입력에 대해 출력 형식(정규식)을 항상 만족해야 한다.
    test("property: 출력은 소문자, 숫자, 하이픈만 사용") {
        val asciiWords = Arb.stringPattern("[A-Za-z0-9 ]{1,40}")
        checkAll(asciiWords) { input ->
            val slug = slugifier.slugify(input)
            Regex("^[a-z0-9]+(-[a-z0-9]+)*$|^$").shouldMatch(slug)
        }
    }

    // property 테스트: 멱등성(두 번 적용해도 결과가 변하지 않음).
    test("property: 멱등성(두 번 적용해도 동일)") {
        val asciiWords = Arb.stringPattern("[A-Za-z0-9 ]{1,40}")
        checkAll(asciiWords) { input ->
            val once = slugifier.slugify(input)
            slugifier.slugify(once) shouldBe once
        }
    }

    // inspectors: 컬렉션의 모든 원소가 조건을 만족하는지 한 번에 검증한다.
    test("inspectors: 각 토큰이 올바른지 전부 확인") {
        val slug = slugifier.slugify("Kotlin 101 For The Brave")
        slug.split("-").forAll { token ->
            Regex("^[a-z0-9]+$").shouldMatch(token)
        }
    }

    // soft assertions: 여러 조건을 검사하고, 실패를 한 번에 보고한다.
    test("soft assertions: 여러 조건을 한 번에 검증") {
        val slug = slugifier.slugify("  Kotlin 101!! ")
        assertSoftly(slug) {
            it shouldStartWith "kotlin"
            it shouldEndWith "101"
            it shouldContain "kotlin-101"
            it shouldNotContain "--"
        }
    }

    // eventually: 일정 시간 동안 재시도하면서 결국 조건이 참이 되는지 확인한다.
    test("eventually: 조건이 참이 될 때까지 재시도") {
        var attempts = 0
        eventually(1.seconds) {
            attempts += 1
            attempts shouldBe 3
        }
    }
})

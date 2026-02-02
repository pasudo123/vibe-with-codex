package com.vibewithcodex.core

import io.kotest.assertions.timing.eventually
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
import java.time.Duration

class SlugifierTest : FunSpec({
    val slugifier = Slugifier()

    context("data-driven examples") {
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

    test("property: output only uses lowercase letters, digits, and hyphens") {
        val asciiWords = Arb.stringPattern("[A-Za-z0-9 ]{1,40}")
        checkAll(asciiWords) { input ->
            val slug = slugifier.slugify(input)
            slug shouldMatch Regex("^[a-z0-9]+(-[a-z0-9]+)*$|^$")
        }
    }

    test("property: idempotent") {
        val asciiWords = Arb.stringPattern("[A-Za-z0-9 ]{1,40}")
        checkAll(asciiWords) { input ->
            val once = slugifier.slugify(input)
            slugifier.slugify(once) shouldBe once
        }
    }

    test("inspectors: every token is clean") {
        val slug = slugifier.slugify("Kotlin 101 For The Brave")
        slug.split("-").forAll { token ->
            token shouldMatch Regex("^[a-z0-9]+$")
        }
    }

    test("soft assertions: validate multiple expectations in one run") {
        val slug = slugifier.slugify("  Kotlin 101!! ")
        assertSoftly(slug) {
            it shouldStartWith "kotlin"
            it shouldEndWith "101"
            it shouldContain "kotlin-101"
            it shouldNotContain "--"
        }
    }

    test("eventually: retry until condition becomes true") {
        var attempts = 0
        eventually(Duration.ofSeconds(1), Duration.ofMillis(10)) {
            attempts += 1
            attempts shouldBe 3
        }
    }
})

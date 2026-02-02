package com.vibewithcodex

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class VibeWithCodexApplicationTests : FunSpec({
    extensions(SpringExtension)

    test("spring context loads") {
        context shouldNotBe null
    }
}) {
    @Autowired
    lateinit var context: ApplicationContext
}

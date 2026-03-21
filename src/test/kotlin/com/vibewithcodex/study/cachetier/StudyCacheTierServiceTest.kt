package com.vibewithcodex.study.cachetier

import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CacheSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "study.cachetier.local-ttl-seconds=1",
        "study.cachetier.redis-default-ttl-seconds=1",
    ],
)
class StudyCacheTierServiceTest : FunSpec() {

    @Autowired
    lateinit var studyCacheTierService: StudyCacheTierService

    init {
        extensions(SpringExtension)

        beforeTest {
            studyCacheTierService.clearForTest()
        }

        test("local miss, redis hit, then local hit") {
            studyCacheTierService.seedRedis(key = "product:1", value = "apple", ttlSeconds = 5)

            val first = studyCacheTierService.getData("product:1")
            first.source shouldBe CacheSource.REDIS
            first.value shouldBe "apple"

            val second = studyCacheTierService.getData("product:1")
            second.source shouldBe CacheSource.LOCAL
            second.value shouldBe "apple"
        }

        test("both local and redis miss") {
            val response = studyCacheTierService.getData("product:missing")

            response.source shouldBe CacheSource.MISS
            response.value.shouldBeNull()
        }

        test("local ttl expiration forces redis lookup again") {
            studyCacheTierService.seedRedis(key = "product:2", value = "banana", ttlSeconds = 5)

            studyCacheTierService.getData("product:2").source shouldBe CacheSource.REDIS
            studyCacheTierService.getData("product:2").source shouldBe CacheSource.LOCAL

            Thread.sleep(1200)

            studyCacheTierService.getData("product:2").source shouldBe CacheSource.REDIS
        }

        test("redis mock ttl expiration returns miss") {
            studyCacheTierService.seedRedis(key = "product:3", value = "cherry", ttlSeconds = 1)

            Thread.sleep(1200)

            val response = studyCacheTierService.getData("product:3")
            response.source shouldBe CacheSource.MISS
            response.value.shouldBeNull()
        }
    }
}

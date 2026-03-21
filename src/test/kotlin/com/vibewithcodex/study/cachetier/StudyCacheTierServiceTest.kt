package com.vibewithcodex.study.cachetier

import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CacheSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * StudyCacheTierService의 cache-aside 동작 검증 테스트.
 *
 * 테스트에서는 TTL 만료 시나리오를 빠르게 확인하기 위해
 * Local/Redis 기본 TTL을 1초로 낮춰 실행한다.
 */
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
            // 테스트 간 캐시 상태 공유를 방지한다.
            studyCacheTierService.clearForTest()
        }

        test("local miss, redis hit, then local hit") {
            // 1) Redis Mock에 데이터 주입
            studyCacheTierService.seedRedis(key = "product:1", value = "apple", ttlSeconds = 5)

            // 2) 첫 조회는 Local miss -> Redis hit
            val first = studyCacheTierService.getData("product:1")
            first.source shouldBe CacheSource.REDIS
            first.value shouldBe "apple"

            // 3) 같은 키 재조회는 Local hit
            val second = studyCacheTierService.getData("product:1")
            second.source shouldBe CacheSource.LOCAL
            second.value shouldBe "apple"
        }

        test("both local and redis miss") {
            // 어떤 계층에도 데이터가 없으면 MISS를 반환해야 한다.
            val response = studyCacheTierService.getData("product:missing")

            response.source shouldBe CacheSource.MISS
            response.value.shouldBeNull()
        }

        test("local ttl expiration forces redis lookup again") {
            // Redis TTL은 충분히 길게 주고, Local TTL만 먼저 만료시키는 시나리오.
            studyCacheTierService.seedRedis(key = "product:2", value = "banana", ttlSeconds = 5)

            studyCacheTierService.getData("product:2").source shouldBe CacheSource.REDIS
            studyCacheTierService.getData("product:2").source shouldBe CacheSource.LOCAL

            // 테스트 프로퍼티(local TTL=1초) 기준으로 만료 유도.
            Thread.sleep(1200)

            // Local 만료 후에는 다시 Redis에서 읽어와야 한다.
            studyCacheTierService.getData("product:2").source shouldBe CacheSource.REDIS
        }

        test("redis mock ttl expiration returns miss") {
            // Redis TTL도 짧게 설정해 저장소 자체 만료를 검증한다.
            studyCacheTierService.seedRedis(key = "product:3", value = "cherry", ttlSeconds = 1)

            Thread.sleep(1200)

            // Redis 만료 이후에는 Local도 비어있으므로 MISS가 맞다.
            val response = studyCacheTierService.getData("product:3")
            response.source shouldBe CacheSource.MISS
            response.value.shouldBeNull()
        }
    }
}

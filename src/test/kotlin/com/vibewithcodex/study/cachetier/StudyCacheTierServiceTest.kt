package com.vibewithcodex.study.cachetier

import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CacheSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
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
            (first.ttlRemainingSeconds != null && first.ttlRemainingSeconds in 1L..5L) shouldBe true

            // 3) 같은 키 재조회는 Local hit
            val second = studyCacheTierService.getData("product:1")
            second.source shouldBe CacheSource.LOCAL
            second.value shouldBe "apple"
            (second.ttlRemainingSeconds != null && second.ttlRemainingSeconds in 0L..1L) shouldBe true
        }

        test("both local and redis miss") {
            // 어떤 계층에도 데이터가 없으면 MISS를 반환해야 한다.
            val response = studyCacheTierService.getData("product:missing")

            response.source shouldBe CacheSource.MISS
            response.value.shouldBeNull()
            response.ttlRemainingSeconds.shouldBeNull()
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

        test("same key reseed invalidates local and resets effective ttl flow") {
            // 첫 seed 후 조회해 Local 캐시에 값이 올라간 상태를 만든다.
            studyCacheTierService.seedRedis(key = "product:4", value = "old", ttlSeconds = 5)
            studyCacheTierService.getData("product:4").source shouldBe CacheSource.REDIS
            studyCacheTierService.getData("product:4").source shouldBe CacheSource.LOCAL

            // 동일 key를 다시 seed하면 Local 캐시가 무효화되어야 한다.
            studyCacheTierService.seedRedis(key = "product:4", value = "new", ttlSeconds = 1)

            // 무효화가 되었다면 첫 조회는 Redis를 타고 최신 값을 가져와야 한다.
            val afterReseed = studyCacheTierService.getData("product:4")
            afterReseed.source shouldBe CacheSource.REDIS
            afterReseed.value shouldBe "new"

            // 짧은 TTL 만료 후에는 MISS가 되어야 한다.
            Thread.sleep(1200)
            studyCacheTierService.getData("product:4").source shouldBe CacheSource.MISS
        }

        test("manual local clear forces next read to redis") {
            studyCacheTierService.seedRedis(key = "product:5", value = "orange", ttlSeconds = 5)
            studyCacheTierService.getData("product:5").source shouldBe CacheSource.REDIS
            studyCacheTierService.getData("product:5").source shouldBe CacheSource.LOCAL

            studyCacheTierService.clearLocalCache()

            // Local을 비운 직후에는 Redis 경로로 다시 조회되어야 한다.
            val afterClear = studyCacheTierService.getData("product:5")
            afterClear.source shouldBe CacheSource.REDIS
            afterClear.value shouldBe "orange"
        }

        test("concurrent same-key lookup keeps redis access minimal with single-flight") {
            studyCacheTierService.seedRedis(key = "product:concurrent", value = "kiwi", ttlSeconds = 5)

            val pool = Executors.newFixedThreadPool(8)
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(8)
            val sources = Collections.synchronizedList(mutableListOf<CacheSource>())

            repeat(8) {
                pool.submit {
                    try {
                        startLatch.await()
                        val result = studyCacheTierService.getData("product:concurrent")
                        sources.add(result.source)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            startLatch.countDown()
            doneLatch.await()
            pool.shutdown()

            sources.size shouldBe 8
            sources.count { it == CacheSource.REDIS } shouldBe 1
            sources.count { it == CacheSource.MISS } shouldBe 0
        }

        test("local cache stats are exposed for operational checks") {
            studyCacheTierService.seedRedis(key = "product:stats", value = "melon", ttlSeconds = 5)
            studyCacheTierService.getData("product:stats")
            studyCacheTierService.getData("product:stats")

            val stats = studyCacheTierService.getLocalCacheStats()

            stats.requestCount shouldNotBe null
            stats.hitCount shouldNotBe null
            stats.missCount shouldNotBe null
            stats.hitRate shouldNotBe null
            (stats.requestCount >= 2) shouldBe true
            (stats.hitCount >= 1) shouldBe true
        }
    }
}

package com.vibewithcodex.study.cachetier

import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CacheLayer
import com.vibewithcodex.study.cachetier.domain.CacheScenario
import com.vibewithcodex.study.cachetier.domain.CacheTraceResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "study.cachetier.local-ttl-seconds=1",
        "study.cachetier.redis-default-ttl-seconds=1",
        "study.cachetier.local-maximum-size=100",
        "study.cachetier.ttl-jitter-seconds=0",
        "study.cachetier.pod-count=3",
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

        test("LOCAL_REDIS_DB falls back to DB, then warms Redis and Local") {
            studyCacheTierService.upsertDb(key = "product:1", value = "apple")
            Thread.sleep(1200)

            val first = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:1")
            first.source shouldBe CacheLayer.DB
            first.value shouldBe "apple"
            first.version shouldBe 1
            first.trace.map { it.layer } shouldContain CacheLayer.DB

            val second = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:1")
            second.source shouldBe CacheLayer.LOCAL
            second.value shouldBe "apple"

            studyCacheTierService.clearPodLocalCache("pod-1")

            val third = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:1")
            third.source shouldBe CacheLayer.REDIS
            third.value shouldBe "apple"
        }

        test("LOCAL_REDIS does not use DB fallback when Redis misses") {
            studyCacheTierService.upsertDb(key = "product:2", value = "banana")
            Thread.sleep(1200)

            val response = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS, "pod-1", "product:2")

            response.source shouldBe CacheLayer.MISS
            response.value.shouldBeNull()
            response.version.shouldBeNull()
            response.trace.any { it.layer == CacheLayer.DB && it.result == CacheTraceResult.SKIP } shouldBe true
        }

        test("LOCAL_DB skips Redis and reads only DB after Local miss") {
            studyCacheTierService.seedRedis(key = "product:3", value = "redis-only", ttlSeconds = 5)

            val redisOnly = studyCacheTierService.getData(CacheScenario.LOCAL_DB, "pod-1", "product:3")
            redisOnly.source shouldBe CacheLayer.MISS
            studyCacheTierService.getRedisAccessCount("product:3") shouldBe 0

            studyCacheTierService.upsertDb(key = "product:3", value = "cherry")

            val dbBacked = studyCacheTierService.getData(CacheScenario.LOCAL_DB, "pod-1", "product:3")
            dbBacked.source shouldBe CacheLayer.DB
            dbBacked.value shouldBe "cherry"
            studyCacheTierService.getRedisAccessCount("product:3") shouldBe 0
        }

        test("each pod owns an independent Local Cache") {
            studyCacheTierService.upsertDb(key = "product:4", value = "durian")

            studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:4").source shouldBe CacheLayer.REDIS
            studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:4").source shouldBe CacheLayer.LOCAL

            val pod2FirstRead = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-2", "product:4")

            pod2FirstRead.source shouldBe CacheLayer.REDIS
            pod2FirstRead.trace.first().result shouldBe CacheTraceResult.MISS
        }

        test("DB update with invalidation refreshes stale pod Local Cache") {
            studyCacheTierService.upsertDb(key = "product:5", value = "old")
            studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:5").source shouldBe CacheLayer.REDIS
            studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:5").source shouldBe CacheLayer.LOCAL

            studyCacheTierService.upsertDb(key = "product:5", value = "new", publishInvalidation = true)

            val afterInvalidation = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:5")
            afterInvalidation.source shouldBe CacheLayer.REDIS
            afterInvalidation.value shouldBe "new"
            afterInvalidation.version shouldBe 2
        }

        test("missing invalidation can leave a pod serving stale Local Cache") {
            studyCacheTierService.upsertDb(key = "product:6", value = "old")
            studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:6").source shouldBe CacheLayer.REDIS
            val localBeforeUpdate = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:6")
            localBeforeUpdate.source shouldBe CacheLayer.LOCAL
            localBeforeUpdate.value shouldBe "old"

            studyCacheTierService.upsertDb(key = "product:6", value = "new", publishInvalidation = false)

            val stale = studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:6")
            stale.source shouldBe CacheLayer.LOCAL
            stale.value shouldBe "old"
            stale.version shouldBe 1
        }

        test("same-key concurrent Local miss uses single-flight per pod") {
            studyCacheTierService.seedRedis(key = "product:concurrent", value = "kiwi", ttlSeconds = 5)

            val pool = Executors.newFixedThreadPool(8)
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(8)
            val sources = Collections.synchronizedList(mutableListOf<CacheLayer>())

            repeat(8) {
                pool.submit {
                    try {
                        startLatch.await()
                        val result = studyCacheTierService.getData(
                            CacheScenario.LOCAL_REDIS_DB,
                            "pod-1",
                            "product:concurrent",
                        )
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
            sources.count { it == CacheLayer.REDIS } shouldBe 1
            sources.count { it == CacheLayer.LOCAL } shouldBe 7
            studyCacheTierService.getRedisAccessCount("product:concurrent") shouldBe 1
        }

        test("pod Local Cache stats are exposed") {
            studyCacheTierService.upsertDb(key = "product:stats", value = "melon")
            studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:stats")
            studyCacheTierService.getData(CacheScenario.LOCAL_REDIS_DB, "pod-1", "product:stats")

            val stats = studyCacheTierService.getLocalCacheStats("pod-1")

            stats.podId shouldBe "pod-1"
            (stats.requestCount >= 2) shouldBe true
            (stats.hitCount >= 1) shouldBe true
            (stats.estimatedSize >= 1) shouldBe true
        }
    }
}

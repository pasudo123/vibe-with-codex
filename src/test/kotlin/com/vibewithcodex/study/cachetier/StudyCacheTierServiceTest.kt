package com.vibewithcodex.study.cachetier

import com.vibewithcodex.study.cachetier.application.StudyCacheTierService
import com.vibewithcodex.study.cachetier.domain.CachePattern
import com.vibewithcodex.study.cachetier.domain.CacheSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "study.cachetier.local-ttl-seconds=2",
        "study.cachetier.refresh-after-write-seconds=1",
        "study.cachetier.local-maximum-size=100",
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

        test("cache-aside loads origin data and then returns local cache hit") {
            studyCacheTierService.putData(key = "product:1", value = "apple")

            val first = studyCacheTierService.getWithCacheAside("product:1")
            first.pattern shouldBe CachePattern.CACHE_ASIDE
            first.source shouldBe CacheSource.LOADER
            first.value shouldBe "apple"

            val second = studyCacheTierService.getWithCacheAside("product:1")
            second.source shouldBe CacheSource.LOCAL_CACHE
            second.value shouldBe "apple"

            val stats = studyCacheTierService.getStats("cache-aside")
            stats.loaderCallCount shouldBe 1
            stats.hitCount shouldBe 1
            stats.missCount shouldBe 1
        }

        test("cache-aside returns miss when origin data does not exist") {
            val response = studyCacheTierService.getWithCacheAside("product:missing")

            response.pattern shouldBe CachePattern.CACHE_ASIDE
            response.source shouldBe CacheSource.MISS
            response.value.shouldBeNull()
        }

        test("loading cache calls loader automatically on miss") {
            studyCacheTierService.putData(key = "product:2", value = "banana")

            val first = studyCacheTierService.getWithLoadingCache("product:2")
            first.pattern shouldBe CachePattern.LOADING_CACHE
            first.source shouldBe CacheSource.LOADER
            first.value shouldBe "banana"

            val second = studyCacheTierService.getWithLoadingCache("product:2")
            second.source shouldBe CacheSource.LOCAL_CACHE
            second.value shouldBe "banana"

            val stats = studyCacheTierService.getStats("loading")
            stats.loaderCallCount shouldBe 1
        }

        test("loading cache can generate fallback value for missing origin data") {
            val response = studyCacheTierService.getWithLoadingCache("product:generated")

            response.source shouldBe CacheSource.LOADER
            response.value shouldBe "generated:product:generated"
            studyCacheTierService.getStats("loading").loaderCallCount shouldBe 1
        }

        test("refreshAfterWrite cache can refresh a cached value from origin") {
            studyCacheTierService.putData(key = "product:3", value = "old")

            val first = studyCacheTierService.getWithRefreshAfterWrite("product:3")
            first.pattern shouldBe CachePattern.REFRESH_AFTER_WRITE
            first.source shouldBe CacheSource.LOADER
            first.value shouldBe "old"

            studyCacheTierService.putData(key = "product:3", value = "new", invalidateCaches = false)

            val beforeRefresh = studyCacheTierService.getWithRefreshAfterWrite("product:3")
            beforeRefresh.source shouldBe CacheSource.LOCAL_CACHE
            beforeRefresh.value shouldBe "old"

            val refreshed = studyCacheTierService.refreshNow("product:3")
            refreshed.value shouldBe "new"

            val afterRefresh = studyCacheTierService.getWithRefreshAfterWrite("product:3")
            afterRefresh.source shouldBe CacheSource.LOCAL_CACHE
            afterRefresh.value shouldBe "new"
        }

        test("manual invalidate removes a specific cache entry") {
            studyCacheTierService.putData(key = "product:4", value = "orange")
            studyCacheTierService.getWithCacheAside("product:4").source shouldBe CacheSource.LOADER
            studyCacheTierService.getWithCacheAside("product:4").source shouldBe CacheSource.LOCAL_CACHE

            studyCacheTierService.invalidate("cache-aside", "product:4")

            val afterInvalidate = studyCacheTierService.getWithCacheAside("product:4")
            afterInvalidate.source shouldBe CacheSource.LOADER
            afterInvalidate.value shouldBe "orange"
            studyCacheTierService.getStats("cache-aside").loaderCallCount shouldBe 2
        }

        test("stats expose request, hit, miss, and cache size") {
            studyCacheTierService.putData(key = "product:stats", value = "melon")
            studyCacheTierService.getWithCacheAside("product:stats")
            studyCacheTierService.getWithCacheAside("product:stats")

            val stats = studyCacheTierService.getStats("cache-aside")

            stats.cacheName shouldBe "cache-aside"
            (stats.requestCount >= 2) shouldBe true
            (stats.hitCount >= 1) shouldBe true
            (stats.missCount >= 1) shouldBe true
            (stats.estimatedSize >= 1) shouldBe true
        }
    }
}

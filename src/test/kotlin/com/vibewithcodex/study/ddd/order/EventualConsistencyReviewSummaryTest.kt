package com.vibewithcodex.study.ddd.order

import com.vibewithcodex.study.ddd.order.application.EditReviewCommand
import com.vibewithcodex.study.ddd.order.application.ProductReviewSummaryProjector
import com.vibewithcodex.study.ddd.order.application.StudyReviewService
import com.vibewithcodex.study.ddd.order.application.WriteReviewCommand
import com.vibewithcodex.study.ddd.order.domain.ProductId
import com.vibewithcodex.study.ddd.order.infra.InMemoryAsyncReviewEventPublisher
import com.vibewithcodex.study.ddd.order.infra.InMemoryProductReviewSummaryRepository
import com.vibewithcodex.study.ddd.order.infra.InMemoryReviewRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class EventualConsistencyReviewSummaryTest : FunSpec({
    test("review write/edit is reflected asynchronously to product review summary") {
        InMemoryAsyncReviewEventPublisher(processingDelayMillis = 120).use { eventPublisher ->
            val reviewRepository = InMemoryReviewRepository()
            val summaryRepository = InMemoryProductReviewSummaryRepository()
            val projector = ProductReviewSummaryProjector(summaryRepository)
            val reviewService = StudyReviewService(reviewRepository, eventPublisher)
            eventPublisher.subscribe(projector)

            reviewService.writeReview(
                WriteReviewCommand(
                    reviewId = "review-1",
                    productId = "product-1",
                    reviewerId = "member-1",
                    score = 5,
                    content = "excellent",
                ),
            )

            // 이벤트가 비동기로 처리되므로 즉시 일치하지 않을 수 있다.
            summaryRepository.findByProductId(ProductId.of("product-1")) shouldBe null

            val afterWrite = awaitSummary(summaryRepository, "product-1") { summary ->
                summary.reviewCount == 1 && summary.averageScore == 5.0
            }
            afterWrite.reviewCount shouldBe 1
            afterWrite.averageScore shouldBe (5.0 plusOrMinus 0.001)

            reviewService.editReview(
                EditReviewCommand(
                    reviewId = "review-1",
                    score = 3,
                    content = "average",
                ),
            )

            val afterEdit = awaitSummary(summaryRepository, "product-1") { summary ->
                summary.reviewCount == 1 && summary.averageScore == 3.0
            }
            afterEdit.reviewCount shouldBe 1
            afterEdit.averageScore shouldBe (3.0 plusOrMinus 0.001)
        }
    }
})

private fun awaitSummary(
    repository: InMemoryProductReviewSummaryRepository,
    productId: String,
    condition: (com.vibewithcodex.study.ddd.order.domain.ProductReviewSummary) -> Boolean,
): com.vibewithcodex.study.ddd.order.domain.ProductReviewSummary {
    val deadline = System.currentTimeMillis() + 3.seconds.inWholeMilliseconds
    while (System.currentTimeMillis() < deadline) {
        val summary = repository.findByProductId(ProductId.of(productId))
        if (summary != null && condition(summary)) {
            return summary
        }
        Thread.sleep(30)
    }
    error("최종 일관성 반영 대기 시간이 초과되었습니다. 상품 식별자=$productId")
}

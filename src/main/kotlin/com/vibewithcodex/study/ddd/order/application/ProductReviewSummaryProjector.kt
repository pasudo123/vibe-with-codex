package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.ProductReviewSummary

/**
 * Review 이벤트를 ProductReviewSummary 리드모델에 투영한다.
 * 반영 시점은 이벤트 소비 시점이므로, 원본 애그리거트와 시차가 발생할 수 있다.
 */
class ProductReviewSummaryProjector(
    private val summaryRepository: ProductReviewSummaryRepository,
) : ReviewEventSubscriber {
    override fun on(event: ReviewEvent) {
        when (event) {
            is ReviewWrittenEvent -> {
                val current = summaryRepository.findByProductId(event.productId)
                    ?: ProductReviewSummary.empty(event.productId)
                summaryRepository.save(current.applyReviewWritten(event.score))
            }

            is ReviewEditedEvent -> {
                val current = summaryRepository.findByProductId(event.productId)
                    ?: ProductReviewSummary.empty(event.productId)
                summaryRepository.save(current.applyReviewEdited(event.oldScore, event.newScore))
            }
        }
    }
}

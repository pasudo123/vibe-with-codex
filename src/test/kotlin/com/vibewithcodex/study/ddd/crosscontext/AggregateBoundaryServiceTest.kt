package com.vibewithcodex.study.ddd.crosscontext

import com.vibewithcodex.study.ddd.ordering.application.AddressCommand
import com.vibewithcodex.study.ddd.ordering.application.ChangeShippingInfoCommand
import com.vibewithcodex.study.ddd.shipping.application.CreateDeliveryTrackingCommand
import com.vibewithcodex.study.ddd.review.application.EditReviewCommand
import com.vibewithcodex.study.ddd.ordering.application.OrderLineCommand
import com.vibewithcodex.study.ddd.ordering.application.OrdererCommand
import com.vibewithcodex.study.ddd.ordering.application.PlaceOrderCommand
import com.vibewithcodex.study.ddd.ordering.application.ReceiverCommand
import com.vibewithcodex.study.ddd.catalog.application.RegisterProductCommand
import com.vibewithcodex.study.ddd.ordering.application.ShippingInfoCommand
import com.vibewithcodex.study.ddd.shipping.application.StudyDeliveryTrackingService
import com.vibewithcodex.study.ddd.ordering.application.StudyOrderService
import com.vibewithcodex.study.ddd.catalog.application.StudyProductService
import com.vibewithcodex.study.ddd.review.application.StudyReviewService
import com.vibewithcodex.study.ddd.review.application.WriteReviewCommand
import com.vibewithcodex.study.ddd.shipping.domain.DeliveryStatus
import com.vibewithcodex.study.ddd.ordering.domain.OrderStatus
import com.vibewithcodex.study.ddd.catalog.domain.ProductStatus
import com.vibewithcodex.study.ddd.review.domain.ReviewId
import com.vibewithcodex.study.ddd.shipping.infra.InMemoryDeliveryTrackingRepository
import com.vibewithcodex.study.ddd.ordering.infra.InMemoryOrderRepository
import com.vibewithcodex.study.ddd.catalog.infra.InMemoryProductRepository
import com.vibewithcodex.study.ddd.review.infra.InMemoryReviewRepository
import com.vibewithcodex.study.ddd.shared.domain.OrderId
import com.vibewithcodex.study.ddd.shared.domain.ProductId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.transaction.annotation.Transactional

class AggregateBoundaryServiceTest : FunSpec({
    test("review changes do not modify product aggregate state") {
        val productRepository = InMemoryProductRepository()
        val reviewRepository = InMemoryReviewRepository()
        val productService = StudyProductService(productRepository)
        val reviewService = StudyReviewService(reviewRepository)

        productService.registerProduct(
            RegisterProductCommand(
                productId = "product-1",
                categoryId = "cat-1",
                name = "Apple",
                price = 2_000,
            ),
        )
        reviewService.writeReview(
            WriteReviewCommand(
                reviewId = "review-1",
                productId = "product-1",
                reviewerId = "member-1",
                score = 5,
                content = "excellent",
            ),
        )

        reviewService.editReview(
            EditReviewCommand(
                reviewId = "review-1",
                score = 3,
                content = "average",
            ),
        )

        val product = productRepository.findById(ProductId.of("product-1"))
            .shouldNotBeNull()
        product.status shouldBe ProductStatus.SELLING
        product.price.amount shouldBe 2_000
    }

    test("product changes do not modify review aggregate state") {
        val productRepository = InMemoryProductRepository()
        val reviewRepository = InMemoryReviewRepository()
        val productService = StudyProductService(productRepository)
        val reviewService = StudyReviewService(reviewRepository)

        productService.registerProduct(
            RegisterProductCommand(
                productId = "product-2",
                categoryId = "cat-1",
                name = "Orange",
                price = 3_000,
            ),
        )
        reviewService.writeReview(
            WriteReviewCommand(
                reviewId = "review-2",
                productId = "product-2",
                reviewerId = "member-1",
                score = 4,
                content = "good",
            ),
        )

        productService.markSoldOut("product-2")

        val review = reviewRepository.findById(ReviewId.of("review-2"))
            .shouldNotBeNull()
        review.content shouldBe "good"
        review.score.value shouldBe 4
    }

    test("delivery tracking changes do not modify order aggregate state") {
        val orderRepository = InMemoryOrderRepository()
        val deliveryTrackingRepository = InMemoryDeliveryTrackingRepository()
        val orderService = StudyOrderService(orderRepository)
        val deliveryTrackingService = StudyDeliveryTrackingService(deliveryTrackingRepository)

        orderService.placeOrder(validPlaceOrderCommand(orderId = "order-1"))
        deliveryTrackingService.createDeliveryTracking(
            CreateDeliveryTrackingCommand(
                orderId = "order-1",
                trackingNumber = "track-1",
                carrier = "cj-logistics",
            ),
        )
        deliveryTrackingService.markInTransit("order-1")
        val tracking = deliveryTrackingService.markDelivered("order-1")

        tracking.status shouldBe DeliveryStatus.DELIVERED
        val order = orderRepository.findById(OrderId.of("order-1")).shouldNotBeNull()
        order.status shouldBe OrderStatus.CREATED
    }

    test("aggregate application services expose transactional boundaries") {
        StudyOrderService::class.java.getDeclaredMethod("placeOrder", PlaceOrderCommand::class.java)
            .isAnnotationPresent(Transactional::class.java) shouldBe true
        StudyOrderService::class.java.getDeclaredMethod("changeShippingInfo", ChangeShippingInfoCommand::class.java)
            .isAnnotationPresent(Transactional::class.java) shouldBe true

        StudyProductService::class.java.getDeclaredMethod("registerProduct", RegisterProductCommand::class.java)
            .isAnnotationPresent(Transactional::class.java) shouldBe true
        StudyReviewService::class.java.getDeclaredMethod("writeReview", WriteReviewCommand::class.java)
            .isAnnotationPresent(Transactional::class.java) shouldBe true
        StudyDeliveryTrackingService::class.java.getDeclaredMethod("markDelivered", String::class.java)
            .isAnnotationPresent(Transactional::class.java) shouldBe true
    }
})

private fun validPlaceOrderCommand(orderId: String): PlaceOrderCommand =
    PlaceOrderCommand(
        orderId = orderId,
        orderer = OrdererCommand(
            memberId = "member-1",
            name = "Alice",
        ),
        shippingInfo = ShippingInfoCommand(
            receiver = ReceiverCommand(
                name = "Alice",
                phoneNumber = "010-0000-0000",
            ),
            address = AddressCommand(
                zipCode = "01234",
                address1 = "Seoul road 1",
                address2 = "101-1001",
            ),
            message = "leave at front door",
        ),
        orderLines = listOf(
            OrderLineCommand(productId = "product-1", unitPrice = 2_000, quantity = 2),
            OrderLineCommand(productId = "product-2", unitPrice = 5_000, quantity = 1),
        ),
    )

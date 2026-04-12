package com.vibewithcodex.study.ddd.crosscontext

import com.vibewithcodex.study.ddd.catalog.domain.Category
import com.vibewithcodex.study.ddd.catalog.domain.CategoryId
import com.vibewithcodex.study.ddd.catalog.domain.Product
import com.vibewithcodex.study.ddd.catalog.domain.ProductStatus
import com.vibewithcodex.study.ddd.member.domain.Member
import com.vibewithcodex.study.ddd.member.domain.MemberGrade
import com.vibewithcodex.study.ddd.ordering.domain.Address
import com.vibewithcodex.study.ddd.ordering.domain.PaymentInfo
import com.vibewithcodex.study.ddd.ordering.domain.PaymentMethod
import com.vibewithcodex.study.ddd.ordering.domain.Receiver
import com.vibewithcodex.study.ddd.ordering.domain.ShippingInfo
import com.vibewithcodex.study.ddd.review.domain.Review
import com.vibewithcodex.study.ddd.review.domain.ReviewId
import com.vibewithcodex.study.ddd.review.domain.ReviewScore
import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException
import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderStateException
import com.vibewithcodex.study.ddd.shared.domain.MemberId
import com.vibewithcodex.study.ddd.shared.domain.Money
import com.vibewithcodex.study.ddd.shared.domain.OrderId
import com.vibewithcodex.study.ddd.shared.domain.ProductId
import com.vibewithcodex.study.ddd.shipping.domain.DeliveryStatus
import com.vibewithcodex.study.ddd.shipping.domain.DeliveryTracking
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class CommerceDomainModelTest : FunSpec({
    test("category supports rename with invariant") {
        val category = Category.create(
            id = CategoryId.of("cat-1"),
            name = "Food",
        )

        category.rename("Fresh Food")
        category.name shouldBe "Fresh Food"

        shouldThrow<InvalidOrderException> {
            category.rename("")
        }
    }

    test("product changes status and mutable attributes via domain methods") {
        val product = Product.create(
            id = ProductId.of("product-1"),
            categoryId = CategoryId.of("cat-1"),
            name = "Apple",
            price = Money.of(2_000),
        )

        product.changePrice(Money.of(2_500))
        product.changeCategory(CategoryId.of("cat-2"))
        product.markSoldOut()

        product.price shouldBe Money.of(2_500)
        product.categoryId shouldBe CategoryId.of("cat-2")
        product.status shouldBe ProductStatus.SOLD_OUT

        product.resumeSale()
        product.status shouldBe ProductStatus.SELLING
    }

    test("review validates score and content rules") {
        shouldThrow<InvalidOrderException> {
            ReviewScore.of(0)
        }

        val review = Review.create(
            id = ReviewId.of("review-1"),
            productId = ProductId.of("product-1"),
            reviewerId = MemberId.of("member-1"),
            score = ReviewScore.of(5),
            content = "great product",
            createdAt = Instant.parse("2026-04-12T10:00:00Z"),
        )
        review.score shouldBe ReviewScore.of(5)

        review.edit(newScore = ReviewScore.of(4), newContent = "still good")
        review.score shouldBe ReviewScore.of(4)
    }

    test("payment info requires approved amount and transaction id") {
        val payment = PaymentInfo(
            method = PaymentMethod.CARD,
            approvedAmount = Money.of(10_000),
            transactionId = "tx-1",
            approvedAt = Instant.parse("2026-04-12T11:00:00Z"),
        )
        payment.approvedAmount shouldBe Money.of(10_000)

        shouldThrow<InvalidOrderException> {
            PaymentInfo(
                method = PaymentMethod.CARD,
                approvedAmount = Money.zero(),
                transactionId = "tx-2",
                approvedAt = Instant.now(),
            )
        }
    }

    test("member grade can be changed by domain method") {
        val member = Member.create(
            id = MemberId.of("member-1"),
            name = "Alice",
        )
        member.grade shouldBe MemberGrade.BRONZE

        member.changeGrade(MemberGrade.GOLD)
        member.grade shouldBe MemberGrade.GOLD
    }

    test("shipping info is composed with receiver and address values") {
        val shippingInfo = ShippingInfo(
            receiver = Receiver(
                name = "Alice",
                phoneNumber = "010-0000-0000",
            ),
            address = Address(
                zipCode = "01234",
                address1 = "Seoul road 1",
                address2 = "101-1001",
            ),
            message = "leave at door",
        )

        shippingInfo.receiver.name shouldBe "Alice"
        shippingInfo.address.zipCode shouldBe "01234"
    }

    test("delivery tracking enforces status transition order") {
        val tracking = DeliveryTracking.create(
            orderId = OrderId.of("order-1"),
            trackingNumber = "track-1",
            carrier = "cj-logistics",
        )
        tracking.status shouldBe DeliveryStatus.READY

        tracking.markInTransit(Instant.parse("2026-04-12T12:00:00Z"))
        tracking.status shouldBe DeliveryStatus.IN_TRANSIT

        tracking.markDelivered(Instant.parse("2026-04-13T09:00:00Z"))
        tracking.status shouldBe DeliveryStatus.DELIVERED

        shouldThrow<InvalidOrderStateException> {
            tracking.markInTransit()
        }
    }
})

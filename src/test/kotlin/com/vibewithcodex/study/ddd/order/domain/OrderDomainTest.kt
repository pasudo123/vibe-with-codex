package com.vibewithcodex.study.ddd.order.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant

class OrderDomainTest : FunSpec({
    test("order is created in a valid state when required fields are provided") {
        val order = Order.create(
            id = OrderId.of("order-1"),
            orderer = Orderer(memberId = MemberId.of("member-1"), name = "Alice"),
            orderLines = listOf(
                OrderLine(productId = ProductId.of("product-1"), unitPrice = Money.of(2_000), quantity = 2),
                OrderLine(productId = ProductId.of("product-2"), unitPrice = Money.of(5_000), quantity = 1),
            ),
            shippingInfo = ShippingInfo(
                receiver = Receiver(
                    name = "Alice",
                    phoneNumber = "010-0000-0000",
                ),
                address = Address(
                    zipCode = "01234",
                    address1 = "Seoul road 1",
                    address2 = "101-1001",
                ),
                message = "door lock #1234",
            ),
            createdAt = Instant.parse("2026-04-12T00:00:00Z"),
        )

        order.status shouldBe OrderStatus.CREATED
        order.totalAmount shouldBe Money.of(9_000)
        order.canceledAt shouldBe null
    }

    test("order creation fails when order lines are empty") {
        shouldThrow<InvalidOrderException> {
            Order.create(
                id = OrderId.of("order-1"),
                orderer = Orderer(memberId = MemberId.of("member-1"), name = "Alice"),
                orderLines = emptyList(),
                shippingInfo = ShippingInfo(
                    receiver = Receiver(
                        name = "Alice",
                        phoneNumber = "010-0000-0000",
                    ),
                    address = Address(
                        zipCode = "01234",
                        address1 = "Seoul road 1",
                    ),
                ),
            )
        }
    }

    test("value object validates invariants on construction") {
        shouldThrow<InvalidOrderException> {
            Money.of(-1)
        }

        shouldThrow<InvalidOrderException> {
            OrderLine(productId = ProductId.of("product-1"), unitPrice = Money.of(1_000), quantity = 0)
        }
    }

    test("shipping address cannot be changed after cancel") {
        val order = createSampleOrder(orderId = "order-1")
        order.cancel(canceledTime = Instant.parse("2026-04-12T01:00:00Z"))

        shouldThrow<InvalidOrderStateException> {
            order.changeShippingInfo(
                ShippingInfo(
                    receiver = Receiver(
                        name = "Bob",
                        phoneNumber = "010-1234-5678",
                    ),
                    address = Address(
                        zipCode = "99999",
                        address1 = "Busan road 2",
                    ),
                ),
            )
        }
    }

    test("value objects are compared by values") {
        val left = ShippingInfo(
            receiver = Receiver(
                name = "Alice",
                phoneNumber = "010-0000-0000",
            ),
            address = Address(
                zipCode = "01234",
                address1 = "Seoul road 1",
                address2 = "101-1001",
            ),
        )
        val right = ShippingInfo(
            receiver = Receiver(
                name = "Alice",
                phoneNumber = "010-0000-0000",
            ),
            address = Address(
                zipCode = "01234",
                address1 = "Seoul road 1",
                address2 = "101-1001",
            ),
        )

        left shouldBe right
    }

    test("entities can share the same identity value but remain different instances") {
        val first = createSampleOrder(orderId = "order-1")
        val second = createSampleOrder(orderId = "order-1")

        first.id shouldBe second.id
        first shouldNotBe second
    }
})

private fun createSampleOrder(orderId: String): Order =
    Order.create(
        id = OrderId.of(orderId),
        orderer = Orderer(memberId = MemberId.of("member-1"), name = "Alice"),
        orderLines = listOf(
            OrderLine(productId = ProductId.of("product-1"), unitPrice = Money.of(3_000), quantity = 2),
        ),
        shippingInfo = ShippingInfo(
            receiver = Receiver(
                name = "Alice",
                phoneNumber = "010-0000-0000",
            ),
            address = Address(
                zipCode = "01234",
                address1 = "Seoul road 1",
                address2 = "101-1001",
            ),
        ),
    )

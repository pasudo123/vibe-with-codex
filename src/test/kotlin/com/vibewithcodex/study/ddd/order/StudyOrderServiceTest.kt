package com.vibewithcodex.study.ddd.order

import com.vibewithcodex.study.ddd.order.application.AddressCommand
import com.vibewithcodex.study.ddd.order.application.ChangeShippingInfoCommand
import com.vibewithcodex.study.ddd.order.application.OrderLineCommand
import com.vibewithcodex.study.ddd.order.application.OrderNotFoundException
import com.vibewithcodex.study.ddd.order.application.OrdererCommand
import com.vibewithcodex.study.ddd.order.application.PlaceOrderCommand
import com.vibewithcodex.study.ddd.order.application.ReceiverCommand
import com.vibewithcodex.study.ddd.order.application.ShippingInfoCommand
import com.vibewithcodex.study.ddd.order.application.StudyOrderService
import com.vibewithcodex.study.ddd.order.domain.InvalidOrderException
import com.vibewithcodex.study.ddd.order.domain.InvalidOrderStateException
import com.vibewithcodex.study.ddd.order.domain.OrderId
import com.vibewithcodex.study.ddd.order.infra.InMemoryOrderRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class StudyOrderServiceTest : FunSpec({
    lateinit var orderRepository: InMemoryOrderRepository
    lateinit var studyOrderService: StudyOrderService

    beforeTest {
        orderRepository = InMemoryOrderRepository()
        studyOrderService = StudyOrderService(orderRepository)
    }

    test("placeOrder creates aggregate and stores it in repository") {
        val created = studyOrderService.placeOrder(validPlaceOrderCommand())

        val saved = orderRepository.findById(OrderId.of("order-1"))
        saved.shouldNotBeNull()
        saved.id.value shouldBe "order-1"
        saved.totalAmount.amount shouldBe 9_000
        created.id.value shouldBe "order-1"
    }

    test("placeOrder does not bypass domain invariant checks") {
        shouldThrow<InvalidOrderException> {
            studyOrderService.placeOrder(
                validPlaceOrderCommand().copy(orderLines = emptyList()),
            )
        }
    }

    test("changeShippingInfo updates shipping info through domain method") {
        studyOrderService.placeOrder(validPlaceOrderCommand())

        val changed = studyOrderService.changeShippingInfo(
            ChangeShippingInfoCommand(
                orderId = "order-1",
                shippingInfo = ShippingInfoCommand(
                    receiver = ReceiverCommand(
                        name = "Bob",
                        phoneNumber = "010-1234-5678",
                    ),
                    address = AddressCommand(
                        zipCode = "54321",
                        address1 = "Busan road 2",
                        address2 = "201-203",
                    ),
                    message = "call before delivery",
                ),
            ),
        )

        changed.shippingInfo.receiver.name shouldBe "Bob"
        changed.shippingInfo.address.zipCode shouldBe "54321"
    }

    test("changeShippingInfo fails when order is missing") {
        shouldThrow<OrderNotFoundException> {
            studyOrderService.changeShippingInfo(
                ChangeShippingInfoCommand(
                    orderId = "order-999",
                    shippingInfo = ShippingInfoCommand(
                        receiver = ReceiverCommand(
                            name = "Bob",
                            phoneNumber = "010-1234-5678",
                        ),
                        address = AddressCommand(
                            zipCode = "54321",
                            address1 = "Busan road 2",
                        ),
                    ),
                ),
            )
        }
    }

    test("changeShippingInfo cannot change canceled order") {
        studyOrderService.placeOrder(validPlaceOrderCommand())
        val order = orderRepository.findById(OrderId.of("order-1")).shouldNotBeNull()
        order.cancel()
        orderRepository.save(order)

        shouldThrow<InvalidOrderStateException> {
            studyOrderService.changeShippingInfo(
                ChangeShippingInfoCommand(
                    orderId = "order-1",
                    shippingInfo = ShippingInfoCommand(
                        receiver = ReceiverCommand(
                            name = "Bob",
                            phoneNumber = "010-1234-5678",
                        ),
                        address = AddressCommand(
                            zipCode = "54321",
                            address1 = "Busan road 2",
                        ),
                    ),
                ),
            )
        }
    }
})

private fun validPlaceOrderCommand(): PlaceOrderCommand =
    PlaceOrderCommand(
        orderId = "order-1",
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

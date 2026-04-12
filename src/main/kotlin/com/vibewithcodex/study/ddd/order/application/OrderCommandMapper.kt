package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Money
import com.vibewithcodex.study.ddd.order.domain.MemberId
import com.vibewithcodex.study.ddd.order.domain.Order
import com.vibewithcodex.study.ddd.order.domain.OrderId
import com.vibewithcodex.study.ddd.order.domain.OrderLine
import com.vibewithcodex.study.ddd.order.domain.Orderer
import com.vibewithcodex.study.ddd.order.domain.ProductId
import com.vibewithcodex.study.ddd.order.domain.Address
import com.vibewithcodex.study.ddd.order.domain.Receiver
import com.vibewithcodex.study.ddd.order.domain.ShippingInfo

/**
 * Application Command -> Domain 변환기.
 *
 * 서비스에서 복잡한 조립 로직을 분리해 유스케이스 흐름을 단순하게 유지한다.
 */
internal fun PlaceOrderCommand.toDomainOrder(): Order = Order.create(
    id = orderId.toDomainOrderId(),
    orderer = orderer.toDomainOrderer(),
    orderLines = orderLines.map(OrderLineCommand::toDomainOrderLine),
    shippingInfo = shippingInfo.toDomainShippingInfo(),
)

internal fun String.toDomainOrderId(): OrderId = OrderId.of(this)

internal fun ChangeShippingInfoCommand.toDomainShippingInfo(): ShippingInfo =
    shippingInfo.toDomainShippingInfo()

internal fun ShippingInfoCommand.toDomainShippingInfo(): ShippingInfo = ShippingInfo(
    receiver = receiver.toDomainReceiver(),
    address = address.toDomainAddress(),
    message = message,
)

private fun ReceiverCommand.toDomainReceiver(): Receiver = Receiver(
    name = name,
    phoneNumber = phoneNumber,
)

private fun AddressCommand.toDomainAddress(): Address = Address(
    zipCode = zipCode,
    address1 = address1,
    address2 = address2,
)

private fun OrdererCommand.toDomainOrderer(): Orderer = Orderer(
    memberId = MemberId.of(memberId),
    name = name,
)

private fun OrderLineCommand.toDomainOrderLine(): OrderLine = OrderLine(
    productId = ProductId.of(productId),
    unitPrice = Money.of(unitPrice),
    quantity = quantity,
)

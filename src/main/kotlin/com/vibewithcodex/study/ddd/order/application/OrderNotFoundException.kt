package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.OrderId

class OrderNotFoundException(
    orderId: OrderId,
) : NoSuchElementException("주문을 찾을 수 없습니다. 주문 식별자=${orderId.value}")

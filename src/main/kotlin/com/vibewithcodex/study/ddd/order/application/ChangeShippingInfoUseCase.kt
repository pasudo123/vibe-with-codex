package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Order

/**
 * 배송 정보 변경 유스케이스 인터페이스.
 */
interface ChangeShippingInfoUseCase {
    fun changeShippingInfo(command: ChangeShippingInfoCommand): Order
}

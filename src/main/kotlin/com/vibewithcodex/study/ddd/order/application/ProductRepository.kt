package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Product
import com.vibewithcodex.study.ddd.order.domain.ProductId

interface ProductRepository {
    fun save(product: Product)
    fun findById(productId: ProductId): Product?
}

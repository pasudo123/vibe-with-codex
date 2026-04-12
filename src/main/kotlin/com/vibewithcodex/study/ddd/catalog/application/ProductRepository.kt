package com.vibewithcodex.study.ddd.catalog.application

import com.vibewithcodex.study.ddd.catalog.domain.Product
import com.vibewithcodex.study.ddd.shared.domain.ProductId

interface ProductRepository {
    fun save(product: Product)
    fun findById(productId: ProductId): Product?
}

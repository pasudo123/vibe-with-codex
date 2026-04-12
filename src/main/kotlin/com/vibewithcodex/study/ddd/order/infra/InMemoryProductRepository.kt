package com.vibewithcodex.study.ddd.order.infra

import com.vibewithcodex.study.ddd.order.application.ProductRepository
import com.vibewithcodex.study.ddd.order.domain.Product
import com.vibewithcodex.study.ddd.order.domain.ProductId
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

@Repository
class InMemoryProductRepository : ProductRepository {
    private val storage = ConcurrentHashMap<ProductId, Product>()

    override fun save(product: Product) {
        storage[product.id] = product
    }

    override fun findById(productId: ProductId): Product? = storage[productId]
}

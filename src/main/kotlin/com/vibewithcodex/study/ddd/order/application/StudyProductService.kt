package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.CategoryId
import com.vibewithcodex.study.ddd.order.domain.Money
import com.vibewithcodex.study.ddd.order.domain.Product
import com.vibewithcodex.study.ddd.order.domain.ProductId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RegisterProductCommand(
    val productId: String,
    val categoryId: String,
    val name: String,
    val price: Long,
    val description: String = "",
)

/**
 * Product 애그리거트 유스케이스를 조율하는 응용 서비스.
 */
@Service
class StudyProductService(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun registerProduct(command: RegisterProductCommand): Product {
        val product = Product.create(
            id = ProductId.of(command.productId),
            categoryId = CategoryId.of(command.categoryId),
            name = command.name,
            price = Money.of(command.price),
            description = command.description,
        )
        return product.also(productRepository::save)
    }

    @Transactional
    fun changePrice(productId: String, price: Long): Product {
        val id = ProductId.of(productId)
        val product = productRepository.findById(id)
            ?: throw EntityNotFoundException(entity = "상품", id = productId)
        product.changePrice(Money.of(price))
        return product.also(productRepository::save)
    }

    @Transactional
    fun markSoldOut(productId: String): Product {
        val id = ProductId.of(productId)
        val product = productRepository.findById(id)
            ?: throw EntityNotFoundException(entity = "상품", id = productId)
        product.markSoldOut()
        return product.also(productRepository::save)
    }
}

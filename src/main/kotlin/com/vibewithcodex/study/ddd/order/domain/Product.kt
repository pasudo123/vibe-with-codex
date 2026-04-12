package com.vibewithcodex.study.ddd.order.domain

/**
 * 상품 애그리거트 루트.
 * 상품은 식별자로 동일성을 판단하고, 판매 상태/가격/카테고리는 변경될 수 있다.
 * 리뷰는 별도 애그리거트로 관리되며 Product는 Review 컬렉션을 직접 가지지 않는다.
 */
class Product private constructor(
    val id: ProductId,
    categoryId: CategoryId,
    name: String,
    price: Money,
    description: String = "",
) {
    var categoryId: CategoryId = categoryId
        private set

    var name: String = name
        private set

    var price: Money = price
        private set

    var description: String = description
        private set

    var status: ProductStatus = ProductStatus.SELLING
        private set

    fun changeCategory(newCategoryId: CategoryId) {
        categoryId = newCategoryId
    }

    fun changeName(newName: String) {
        if (newName.isBlank()) {
            throw InvalidOrderException("상품 이름은 비어 있을 수 없습니다.")
        }
        name = newName
    }

    fun changePrice(newPrice: Money) {
        price = newPrice
    }

    fun changeDescription(newDescription: String) {
        description = newDescription
    }

    fun markSoldOut() {
        status = ProductStatus.SOLD_OUT
    }

    fun resumeSale() {
        status = ProductStatus.SELLING
    }

    companion object {
        fun create(
            id: ProductId,
            categoryId: CategoryId,
            name: String,
            price: Money,
            description: String = "",
        ): Product {
            if (name.isBlank()) {
                throw InvalidOrderException("상품 이름은 비어 있을 수 없습니다.")
            }
            return Product(
                id = id,
                categoryId = categoryId,
                name = name,
                price = price,
                description = description,
            )
        }
    }
}

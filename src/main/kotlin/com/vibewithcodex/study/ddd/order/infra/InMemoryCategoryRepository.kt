package com.vibewithcodex.study.ddd.order.infra

import com.vibewithcodex.study.ddd.order.application.CategoryRepository
import com.vibewithcodex.study.ddd.order.domain.Category
import com.vibewithcodex.study.ddd.order.domain.CategoryId
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Repository

@Repository
class InMemoryCategoryRepository : CategoryRepository {
    private val storage = ConcurrentHashMap<CategoryId, Category>()

    override fun save(category: Category) {
        storage[category.id] = category
    }

    override fun findById(categoryId: CategoryId): Category? = storage[categoryId]
}

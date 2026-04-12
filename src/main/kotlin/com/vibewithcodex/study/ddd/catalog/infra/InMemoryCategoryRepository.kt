package com.vibewithcodex.study.ddd.catalog.infra

import com.vibewithcodex.study.ddd.catalog.application.CategoryRepository
import com.vibewithcodex.study.ddd.catalog.domain.Category
import com.vibewithcodex.study.ddd.catalog.domain.CategoryId
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

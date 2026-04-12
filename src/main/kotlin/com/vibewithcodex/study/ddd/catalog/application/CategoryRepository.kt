package com.vibewithcodex.study.ddd.catalog.application

import com.vibewithcodex.study.ddd.catalog.domain.Category
import com.vibewithcodex.study.ddd.catalog.domain.CategoryId

interface CategoryRepository {
    fun save(category: Category)
    fun findById(categoryId: CategoryId): Category?
}

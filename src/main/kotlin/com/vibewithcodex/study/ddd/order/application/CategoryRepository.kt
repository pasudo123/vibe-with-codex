package com.vibewithcodex.study.ddd.order.application

import com.vibewithcodex.study.ddd.order.domain.Category
import com.vibewithcodex.study.ddd.order.domain.CategoryId

interface CategoryRepository {
    fun save(category: Category)
    fun findById(categoryId: CategoryId): Category?
}

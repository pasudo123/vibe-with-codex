package com.vibewithcodex.study.ddd.catalog.application

import com.vibewithcodex.study.ddd.catalog.domain.Category
import com.vibewithcodex.study.ddd.catalog.domain.CategoryId
import com.vibewithcodex.study.ddd.shared.application.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CreateCategoryCommand(
    val categoryId: String,
    val name: String,
    val parentCategoryId: String? = null,
)

/**
 * Category 애그리거트 유스케이스를 조율하는 응용 서비스.
 */
@Service
class StudyCategoryService(
    private val categoryRepository: CategoryRepository,
) {
    @Transactional
    fun createCategory(command: CreateCategoryCommand): Category {
        val category = Category.create(
            id = CategoryId.of(command.categoryId),
            name = command.name,
            parentId = command.parentCategoryId?.let(CategoryId::of),
        )
        return category.also(categoryRepository::save)
    }

    @Transactional
    fun renameCategory(categoryId: String, newName: String): Category {
        val id = CategoryId.of(categoryId)
        val category = categoryRepository.findById(id)
            ?: throw EntityNotFoundException(entity = "카테고리", id = categoryId)
        category.rename(newName)
        return category.also(categoryRepository::save)
    }
}

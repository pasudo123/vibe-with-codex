package com.vibewithcodex.study.ddd.catalog.domain

import com.vibewithcodex.study.ddd.shared.domain.InvalidOrderException

/**
 * 상품 분류를 나타내는 엔티티.
 * 카테고리는 식별자로 동일성을 유지하며 이름은 변경될 수 있다.
 */
class Category private constructor(
    val id: CategoryId,
    name: String,
    val parentId: CategoryId? = null,
) {
    var name: String = name
        private set

    fun rename(newName: String) {
        if (newName.isBlank()) {
            throw InvalidOrderException("카테고리 이름은 비어 있을 수 없습니다.")
        }
        name = newName
    }

    companion object {
        fun create(
            id: CategoryId,
            name: String,
            parentId: CategoryId? = null,
        ): Category {
            if (name.isBlank()) {
                throw InvalidOrderException("카테고리 이름은 비어 있을 수 없습니다.")
            }
            return Category(id = id, name = name, parentId = parentId)
        }
    }
}

package com.vibewithcodex.study.ddd.shared.application

class EntityNotFoundException(
    entity: String,
    id: String,
) : NoSuchElementException("${entity}을(를) 찾을 수 없습니다. 식별자=$id")

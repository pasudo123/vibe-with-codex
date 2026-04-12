package com.vibewithcodex.study.ddd.order.application

class EntityNotFoundException(
    entity: String,
    id: String,
) : NoSuchElementException("${entity}을(를) 찾을 수 없습니다. 식별자=$id")

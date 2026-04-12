package com.vibewithcodex.study.ddd.shared.domain

open class InvalidOrderException(
    message: String,
) : IllegalArgumentException(message)

class InvalidOrderStateException(
    message: String,
) : IllegalStateException(message)

package com.pavelov.currenciestest.network

class Response<out T> private constructor(
    val hasError: Boolean,
    val data: T?
) {
    companion object {
        fun <T> success(data: T) = Response(false, data)
        fun <T> error() = Response<T>(true, null)
    }
}
package com.pavelov.currenciestest.network

class Request(
    val url: String,
    val pathParams: Map<String, String>? = null
)
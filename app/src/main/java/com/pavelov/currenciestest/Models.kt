package com.pavelov.currenciestest


data class Rates(
    val baseCurrencyCode: String,
    val currencyRates: List<CurrencyRate>
)

data class CurrencyRate(
    val currencyCode: String,
    val rate: Double
)

data class CurrencyName(
    val currencyCode: String,
    val displayName: String
)

data class CurrencyAmountVO(
    val currencyCode: String,
    val currencyAmount: Double
)

/** Find display name by currency code */
fun List<CurrencyName>?.getDisplayNameFor(currencyCode: String): String =
    this?.firstOrNull { it.currencyCode == currencyCode }?.displayName ?: ""
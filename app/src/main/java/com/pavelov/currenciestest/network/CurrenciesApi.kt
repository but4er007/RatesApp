package com.pavelov.currenciestest.network

import android.util.JsonReader
import com.pavelov.currenciestest.CurrencyName
import com.pavelov.currenciestest.CurrencyRate
import com.pavelov.currenciestest.Rates

class CurrenciesApi(
    private val httpClient: HttpClient
) {

    fun getCurrencies(base: String?) = httpClient.get(
        Request(
            "https://revolut.duckdns.org/latest",
            base?.let { mapOf("base" to base) }),
        BaseRatesDeserializer()
    )

    fun getCurrencyNames() = httpClient.get(
        Request("https://openexchangerates.org/api/currencies.json"),
        CurrencyNamesDeserializer()
    )
}

class CurrencyNamesDeserializer :
    JsonDeserializer<List<CurrencyName>> {
    override fun deserialize(jsonReader: JsonReader): List<CurrencyName> {
        val currencyNames = mutableListOf<CurrencyName>()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            currencyNames.add(
                CurrencyName(
                    jsonReader.nextName(),
                    jsonReader.nextString()
                )
            )
        }
        jsonReader.endObject()
        return currencyNames
    }
}

class BaseRatesDeserializer : JsonDeserializer<Rates> {
    override fun deserialize(jsonReader: JsonReader): Rates {
        var responseBase: String? = null
        val rates = mutableListOf<CurrencyRate>()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "base" -> responseBase = jsonReader.nextString()
                "rates" -> {
                    jsonReader.beginObject()
                    while (jsonReader.hasNext()) {
                        rates.add(
                            CurrencyRate(
                                jsonReader.nextName(),
                                jsonReader.nextDouble()
                            )
                        )
                    }
                    jsonReader.endObject()
                }
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        if (responseBase == null || rates.isEmpty()) throw java.lang.Exception("invalid response")
        return Rates(responseBase, rates)
    }
}
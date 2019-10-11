package com.pavelov.currenciestest.network

import android.util.JsonReader

interface JsonDeserializer<T> {
    @Throws(Exception::class)
    fun deserialize(jsonReader: JsonReader): T
}
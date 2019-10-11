package com.pavelov.currenciestest.network

import android.util.JsonReader
import com.pavelov.currenciestest.utils.loge
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class HttpClient {
    fun <T> get(request: Request, deserializer: JsonDeserializer<T>): Response<T> {
        val params = request.pathParams?.entries?.joinToString(
            prefix = "?",
            separator = "&"
        ) { "${it.key}=${it.value}" } ?: ""
        val url = URL("${request.url}$params")
        var conn: HttpsURLConnection? = null
        var connectionInputStream: InputStream? = null
        var connectionInputStreamReader: InputStreamReader? = null
        var jsonReader: JsonReader? = null
        try {
            conn = url.openConnection() as? HttpsURLConnection
                ?: throw java.lang.Exception("openConnection is null")
            conn.readTimeout = 5000
            conn.connectTimeout = 3000
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException(
                    "failed request, " +
                            "error currencyCode = ${conn.responseCode}, " +
                            "message = ${conn.responseMessage}"
                )
            }
            connectionInputStream =
                conn.inputStream ?: throw java.lang.Exception("null inputStream")
            connectionInputStreamReader = InputStreamReader(connectionInputStream)
            jsonReader = JsonReader(connectionInputStreamReader)
            return Response.success(deserializer.deserialize(jsonReader))
        } catch (ex: Exception) {
            loge("network fail", ex)
            return Response.error()
        } finally {
            jsonReader?.close()
            connectionInputStreamReader?.close()
            connectionInputStream?.close()
            conn?.disconnect()
        }
    }
}
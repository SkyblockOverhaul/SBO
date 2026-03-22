package net.sbo.mod.utils.http

import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray


object Http {
    val jsonParser = Json { ignoreUnknownKeys = true }
    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 10000
    private const val USER_AGENT = "SBO-Kotlin-Mod/1.21"

    /**
     * Sends an asynchronous HTTP GET request.
     *
     * @param urlString The URL to send the request to.
     * @return An [HttpRequestHandle] to which .result() and .error() can be chained.
     */
    fun sendGetRequest(urlString: String): HttpRequestHandle {
        val handle = HttpRequestHandle()
        CompletableFuture.runAsync {
            try {
                val url = URI(urlString).toURL()
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                handleConnection(connection, handle)
            } catch (e: Exception) {
                handle.fail(e)
            }
        }
        return handle
    }

    /**
     * Sends an asynchronous HTTP POST request with a JSON body.
     *
     * @param urlString The URL to send the request to.
     * @param jsonBody The JSON data to be sent in the request body.
     * @return An [HttpRequestHandle].
     */
    fun sendPostRequest(urlString: String, jsonBody: String): HttpRequestHandle {
        val handle = HttpRequestHandle()
        CompletableFuture.runAsync {
            try {
                val url = URI(urlString).toURL()
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    doOutput = true // Important for POST
                    setRequestProperty("Content-Type", "application/json; utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", USER_AGENT)
                }

                // Write JSON body
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(jsonBody) }

                handleConnection(connection, handle)
            } catch (e: Exception) {
                handle.fail(e)
            }
        }
        return handle
    }

    /**
     * Private helper function that processes the response and triggers the appropriate callback.
     */
    private fun handleConnection(connection: HttpURLConnection, handle: HttpRequestHandle) {
        try {
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage ?: ""

            val responseBody = if (responseCode in 200..299) {
                ResponseBody(connection.inputStream)
            } else {
                ResponseBody(connection.errorStream)
            }

            val httpResponse = HttpResponse(responseCode, responseMessage, responseBody)
            handle.complete(httpResponse)

        } catch (e: Exception) {
            handle.fail(e)
        } finally {
            connection.disconnect()
        }
    }

    fun JsonObject.getBoolean(key: String): Boolean {
        return this[key]?.jsonPrimitive?.booleanOrNull ?: false
    }

    fun JsonObject.getString(key: String): String?  {
        return this[key]?.jsonPrimitive?.contentOrNull
    }

    fun JsonObject.getInt(key: String): Int? {
        return this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    }

    fun JsonObject.getLong(key: String): Long? {
        return this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
    }

    fun JsonObject.getDouble(key: String): Double? {
        return this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
    }

    fun JsonObject.getMutableMap(key: String): MutableMap<String, Any>? {
        return this[key]?.jsonObject?.mapValues { entry ->
            when (val value = entry.value) {
                is JsonObject -> value.toMutableMap()
                else -> value.jsonPrimitive.contentOrNull ?: value
            }
        }?.toMutableMap()
    }

    fun JsonObject.getArray(key: String): List<Any>? {
        return this[key]?.jsonArray?.map { element ->
            when (element) {
                is JsonObject -> element.toMutableMap()
                else -> element.jsonPrimitive.contentOrNull ?: element
            }
        }
    }
}

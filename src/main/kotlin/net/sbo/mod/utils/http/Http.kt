package net.sbo.mod.utils.http

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.SharedConstants
import net.sbo.mod.SBOKotlin
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture

object Http {
    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 10000
    private val USER_AGENT = "SBO-Kotlin-Mod/" + SBOKotlin.version + "+" + SharedConstants.getCurrentVersion().name()

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
}

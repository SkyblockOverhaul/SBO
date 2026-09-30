package net.sbo.mod.utils.http

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream

/**
 * Represents the body of an HTTP response.
 * The .string() method reads the body as text.
 */
class ResponseBody(private val inputStream: InputStream?) {
    fun string(): String = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
}

/**
 * Represents the result of an HTTP request.
 * @param code The HTTP status code (e.g., 200, 404).
 * @param message The HTTP status message (e.g., "OK", "Not Found").
 * @param body A [ResponseBody] object for accessing the response content.
 */
data class HttpResponse(
    val code: Int,
    val message: String,
    val body: ResponseBody?
) {
    /** Returns true if the HTTP code is in the success range (200-299). */
    val isSuccessful: Boolean get() = code in 200..299
}

@PublishedApi
internal fun httpErrorOf(response: HttpResponse): Exception {
    val fallback = "HTTP request failed with code: ${response.code} ${response.message}"
    val body = try {
        response.body?.string()
    } catch (_: Exception) {
        null
    }
    if (body.isNullOrBlank()) return Exception(fallback)

    val detail = try {
        val json = Json.parseToJsonElement(body).jsonObject
        val error = json["Error"]?.jsonPrimitive?.contentOrNull
        val message = json["message"]?.jsonPrimitive?.contentOrNull
        when {
            !error.isNullOrBlank() -> error
            !message.isNullOrBlank() -> message
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    return Exception(if (detail == null) fallback else "$detail (HTTP ${response.code})")
}

/**
 * A handle that is returned to register callbacks for success or failure.
 * Enables the chained .result{...}.error{...} syntax.
 */
class HttpRequestHandle {
    // FIX: Changed from 'private' to '@PublishedApi internal' to allow access from the public inline function.
    @PublishedApi
    internal var onResult: ((HttpResponse) -> Unit)? = null

    // FIX: Changed from 'private' to '@PublishedApi internal' for consistency and access by 'fail'.
    @PublishedApi
    internal var onError: ((Exception) -> Unit)? = null

    /**
     * The original result handler for raw HTTP responses.
     */
    fun result(callback: (HttpResponse) -> Unit): HttpRequestHandle {
        this.onResult = callback
        return this
    }

    /**
     * The `onSuccess` block only runs for 2xx responses with a valid JSON body.
     * Any error (network, HTTP error code, or parsing failure) is routed to the .error() block.
     *
     * @param ignoreUnknownKeys If true, the JSON parser will ignore properties in the JSON
     * that do not exist in the target data class. Defaults to false (strict parsing).
     * @param onSuccess The callback to execute with the parsed data object.
     */
    inline fun <reified T> toJson(
        ignoreUnknownKeys: Boolean = false,
        crossinline onSuccess: (T) -> Unit
    ): HttpRequestHandle {
        this.onResult = { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    this.fail(Exception("Cannot parse JSON: Response body was empty."))
                } else {
                    try {
                        val jsonParser = Json { this.ignoreUnknownKeys = ignoreUnknownKeys }
                        val data = jsonParser.decodeFromString<T>(body)
                        onSuccess(data)
                    } catch (e: Exception) {
                        this.fail(e)
                    }
                }
            } else {
                this.fail(httpErrorOf(response))
            }
        }
        return this
    }

    /**
     * The `onSuccess` block only runs for 2xx responses with a valid JSON body.
     * Any error is routed to the .error() block.
     */
    fun toJsonObject(onSuccess: (JsonObject) -> Unit): HttpRequestHandle {
        this.onResult = { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    this.fail(Exception("Cannot parse JSON: Response body was empty."))

                } else {
                    try {
                        val data = Json.parseToJsonElement(body).jsonObject
                        onSuccess(data)
                    } catch (e: Exception) {
                        this.fail(e)
                    }
                }
            } else {
                this.fail(httpErrorOf(response))
            }
        }
        return this
    }

    /**
     * The error handler, which now catches network, HTTP, and parsing errors.
     */
    fun error(callback: (Exception) -> Unit): HttpRequestHandle {
        this.onError = callback
        return this
    }

    internal fun complete(response: HttpResponse) {
        onResult?.invoke(response)
    }

    @PublishedApi
    internal fun fail(exception: Exception) {
        onError?.invoke(exception)
    }
}
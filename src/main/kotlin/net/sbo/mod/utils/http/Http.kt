package net.sbo.mod.utils.http

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.SharedConstants
import net.sbo.mod.SBOKotlin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object Http {
    private const val CONNECT_TIMEOUT = 20_000L
    private const val REQUEST_TIMEOUT = 20_000L

    private val USER_AGENT =
        "SBO-Kotlin-Mod/${SBOKotlin.version}+${SharedConstants.getCurrentVersion().name()}"

    private val EXECUTOR: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2,
        ThreadFactory { runnable ->
            Thread(runnable, "SBO Http Request Thread").apply {
                isDaemon = true
            }
        }
    )

    private val HTTP_3_OR_2: HttpClient.Version = http3WithFallback()

    private val CLIENT: HttpClient = HttpClient.newBuilder()
        .executor(EXECUTOR)
        .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
        .version(HTTP_3_OR_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private fun http3WithFallback(): HttpClient.Version {
        try {
            return HttpClient.Version.valueOf("HTTP_3") // Available since Java 26 (JEP 517: HTTP/3 for the HTTP Client API)
        } catch (ignored: IllegalArgumentException) {
            return HttpClient.Version.HTTP_2 // Fallback to baseline of HTTP/2
        }
    }

    /**
     * Sends an asynchronous HTTP GET request.
     *
     * @param urlString The URL to send the request to.
     * @return An [HttpRequestHandle] to which .result() and .error() can be chained.
     */
    fun sendGetRequest(urlString: String): HttpRequestHandle {
        val handle = HttpRequestHandle()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(urlString))
            .timeout(Duration.ofMillis(REQUEST_TIMEOUT))
            .header("User-Agent", USER_AGENT)
            .GET()
            .build()

        EXECUTOR.execute {
            try {
                val response = CLIENT.send(request, BodyHandlers.ofInputStream())
                val code = response.statusCode()
                handle.complete(
                    HttpResponse(
                        code,
                        statusMessage(code),
                        ResponseBody(response.body())
                    )
                )
            } catch (e: Exception) {
                handle.fail(e)
            }
        }

        return handle
    }

    /**
     * Small matcher to turn HTTP status code to text, since HTTP/2+ and the new HttpClient API does not provide this.
     *
     * Not RFC-perfect by any means and will return a generic message for lot of uncommon/rare/joke HTTP codes, let's hope
     * the backend is not going to return 418 (The "I'm a teapot" HTTP response code)
     */
    private fun statusMessage(code: Int): String {
        return when (code) {
            400 -> "Bad request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not found"
            408 -> "Request timed out"
            409 -> "Conflict"
            418 -> "Invalid request"
            422 -> "Invalid data"
            429 -> "Too many requests"
            500 -> "Server error"
            502 -> "Bad gateway"
            503 -> "Service unavailable"
            504 -> "Gateway timeout"
            else -> "HTTP error $code"
        }
    }

    fun JsonObject.getBoolean(key: String): Boolean {
        return this[key]?.jsonPrimitive?.booleanOrNull ?: false
    }

    fun JsonObject.getString(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull
    }

    fun JsonObject.getInt(key: String): Int? {
        return this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
    }
}

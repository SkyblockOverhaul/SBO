package net.sbo.mod.utils.http

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.SharedConstants
import net.sbo.mod.SBOKotlin
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

object Http {
    private const val CONNECT_TIMEOUT = 30_000L
    private const val REQUEST_TIMEOUT = 30_000L

    private val USER_AGENT =
        "SBO-Kotlin-Mod/${SBOKotlin.version}+${SharedConstants.getCurrentVersion().name()}"

    private val EXECUTOR: ExecutorService = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "SBO Http Request Thread").apply {
            isDaemon = true
        }
    }

    private val HTTP_3_OR_2: HttpClient.Version = http3WithFallback()

    // Bodies are read into memory, the biggest one (Hypixel bazaar) is about 4 MB
    private const val MAX_BODY_BYTES = 10 * 1024 * 1024

    private val CLIENT: HttpClient = HttpClient.newBuilder()
        .executor(EXECUTOR)
        .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    // The JDK keeps own headers on redirects, so requests with an SBO key or session never follow one
    private val NO_REDIRECT_CLIENT: HttpClient = HttpClient.newBuilder()
        .executor(EXECUTOR)
        .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    // Their Date header sets the server time, both run on synced clocks and get called every few minutes anyway
    private val TIME_HOSTS = setOf("api.hypixel.net", URI.create(SBOKotlin.API_URL).host)

    /** How far the PC clock is behind Hypixel and the SBO backend, from the Date header of their last response. */
    @Volatile
    private var clockOffsetMillis = 0L

    /** The real time even when the PC clock is wrong, the PC clock until the first response from one of the [TIME_HOSTS]. */
    fun serverTimeMillis(): Long = System.currentTimeMillis() + clockOffsetMillis

    private fun updateClockOffset(host: String?, date: String?) {
        if (host !in TIME_HOSTS || date == null) return
        val server = try {
            // RFC_1123_DATE_TIME has fixed English names, a formatter from a pattern would follow the locale
            ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        } catch (_: Exception) {
            return
        }
        val offset = (server - System.currentTimeMillis()).let {
            // The header has whole seconds and the request takes a moment, a clock this close is left as it is
            if (abs(it) < 5_000) 0L else it
        }
        if (abs(offset - clockOffsetMillis) > 60_000) {
            SBOKotlin.logger.warn("PC clock is ${-offset / 1000}s off $host, using the server time for SkyBlock dates")
        }
        clockOffsetMillis = offset
    }

    private fun http3WithFallback(): HttpClient.Version {
        return try {
            HttpClient.Version.valueOf("HTTP_3") // Available since Java 26 (JEP 517: HTTP/3 for the HTTP Client API)
        } catch (ignored: IllegalArgumentException) {
            HttpClient.Version.HTTP_2 // Fallback to baseline of HTTP/2
        }
    }

    /**
     * Sends an asynchronous HTTP GET request.
     *
     * @param urlString The URL to send the request to.
     * @param headers Additional request headers, e.g. the `x-sbo-key` header for authenticated SBO endpoints.
     * @return An [HttpRequestHandle] to which .result() and .error() can be chained.
     */
    fun sendGetRequest(urlString: String, headers: Map<String, String> = emptyMap()): HttpRequestHandle =
        send(urlString, headers) { it.GET() }

    /**
     * Sends an asynchronous HTTP POST request with a JSON body.
     *
     * The `Content-Type: application/json` header is set automatically.
     *
     * @param urlString The URL to send the request to.
     * @param body The JSON body to send. Defaults to an empty object for endpoints that take no payload.
     * @param headers Additional request headers, e.g. the `x-sbo-key` header for authenticated SBO endpoints.
     * @return An [HttpRequestHandle] to which .result() and .error() can be chained.
     */
    fun sendPostRequest(
        urlString: String,
        body: String = "{}",
        headers: Map<String, String> = emptyMap()
    ): HttpRequestHandle = send(urlString, headers + ("Content-Type" to "application/json")) {
        it.POST(HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
    }

    private fun decompress(
        body: InputStream,
        contentEncoding: String?
    ): InputStream {
        if (contentEncoding == null) return body

        val peek = PushbackInputStream(body)
        val first = peek.read()
        if (first == -1) return peek
        peek.unread(first)

        var decoded: InputStream = peek

        // Content codings are applied in order, so decode them in reverse order.
        for (encoding in contentEncoding.split(',').asReversed()) {
            decoded = when (encoding.trim().lowercase(Locale.ROOT)) {
                "identity" -> decoded
                "gzip", "x-gzip" -> GZIPInputStream(decoded)
                else -> throw IOException("Unsupported content encoding: $encoding")
            }
        }

        return decoded
    }

    private fun send(
        urlString: String,
        headers: Map<String, String>,
        method: (HttpRequest.Builder) -> HttpRequest.Builder
    ): HttpRequestHandle {
        val handle = HttpRequestHandle()

        try {
            val uri = URI.create(urlString)
            val httpVersion = when {
                // Without TLS the JDK asks for a h2c upgrade, which a local backend answers with 400
                uri.scheme == "http" -> HttpClient.Version.HTTP_1_1
                else -> HTTP_3_OR_2
            }

            val client = if (headers.keys.any { it.startsWith("x-sbo-", ignoreCase = true) }) NO_REDIRECT_CLIENT else CLIENT

            fun request(version: HttpClient.Version): HttpRequest {
                val builder = HttpRequest.newBuilder()
                    .version(version)
                    .uri(uri)
                    .timeout(Duration.ofMillis(REQUEST_TIMEOUT))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Encoding", "gzip")

                headers.forEach { (name, value) -> builder.header(name, value) }
                return method(builder).build()
            }

            val initialRequest = request(httpVersion)
            val initialSend = client.sendAsync(initialRequest, BodyHandlers.ofInputStream())
            val send = if (httpVersion.name == "HTTP_3") {
                // In case of a connection failure with HTTP/3, attempt the same request a single time using HTTP/2.
                initialSend.exceptionallyCompose {
                    // This fallback is deliberately only one level deep: an HTTP/2 failure is returned as-is.
                    client.sendAsync(request(HttpClient.Version.HTTP_2), BodyHandlers.ofInputStream())
                }
            } else {
                initialSend
            }

            send
                .thenAccept { response ->
                    try {
                        val code = response.statusCode()
                        updateClockOffset(uri.host, response.headers().firstValue("Date").orElse(null))
                        val encoding = response.headers().allValues("Content-Encoding").joinToString(",").ifEmpty { null }
                        // The outer use closes the raw body when decompress() throws before returning a stream
                        val body = response.body().use { raw ->
                            decompress(raw, encoding).use { it.readNBytes(MAX_BODY_BYTES + 1) }
                        }
                        if (body.size > MAX_BODY_BYTES) throw IOException("Response from ${uri.host} is larger than ${MAX_BODY_BYTES / 1024 / 1024} MB")

                        val status = statusMessage(code)
                        val responseBody = ResponseBody(body.inputStream())

                        handle.complete(
                            HttpResponse(
                                code,
                                status,
                                responseBody
                            )
                        )
                    } catch (e: Exception) {
                        handle.fail(e)
                    }
                }
                .exceptionally { e ->
                    handle.fail(Exception("HTTP request to $urlString failed", e.cause ?: e))
                    null
                }
        } catch (e: Exception) {
            handle.fail(e)
        }

        return handle
    }

    /**
     * Small matcher to turn HTTP status code to text, since HTTP/2+ and the new HttpClient API does not provide this.
     *
     * Not RFC-perfect by any means and will return a generic message for a lot of uncommon/rare/joke HTTP codes, let's hope
     * the backend is not going to return 418 (The "I'm a teapot" HTTP response code)
     */
    private fun statusMessage(code: Int): String {
        return when (code) {
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            408 -> "Request Timed Out"
            409 -> "Conflict"
            418 -> "Invalid Request"
            422 -> "Invalid Data"
            429 -> "Too Many Requests"
            500 -> "Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"
            else -> "HTTP Error $code"
        }
    }

    fun JsonObject.getBoolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull == true

    fun JsonObject.getString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    fun JsonObject.getInt(key: String): Int? = this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
}

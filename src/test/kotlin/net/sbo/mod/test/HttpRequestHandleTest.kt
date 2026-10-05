package net.sbo.mod.test

import net.sbo.mod.utils.http.HttpRequestHandle
import net.sbo.mod.utils.http.HttpResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HttpRequestHandleTest {
    @Test
    fun resultBeforeCallbacksIsDelivered() {
        val handle = HttpRequestHandle()
        handle.complete(HttpResponse(200, "OK", null))
        var code = 0
        handle.result { code = it.code }
        assertEquals(200, code)
    }

    @Test
    fun failureBeforeCallbacksIsDelivered() {
        val handle = HttpRequestHandle()
        handle.fail(Exception("offline"))
        var message = ""
        handle.result { }.error { message = it.message.orEmpty() }
        assertEquals("offline", message)
    }

    @Test
    fun deliveredOnlyOnce() {
        val handle = HttpRequestHandle()
        var calls = 0
        handle.result { calls++ }
        handle.complete(HttpResponse(200, "OK", null))
        handle.error { }
        assertEquals(1, calls)
    }
}

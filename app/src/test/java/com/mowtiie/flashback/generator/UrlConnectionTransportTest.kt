package com.mowtiie.flashback.generator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** Real sockets on localhost: a request nobody answers must stop as soon as the caller is cancelled. */
class UrlConnectionTransportTest {

    private lateinit var server: ServerSocket
    private val accepted = CountDownLatch(1)
    private val serverStop = AtomicBoolean(false)

    @Before
    fun setUp() {
        server = ServerSocket(0)
    }

    @After
    fun tearDown() {
        serverStop.set(true)
        server.close()
    }

    private fun url() = "http://127.0.0.1:${server.localPort}/"

    /** Reads the request, then never answers. */
    private fun silentServer() = thread(isDaemon = true) {
        runCatching {
            val socket = server.accept()
            accepted.countDown()
            socket.getInputStream().read(ByteArray(4096))
            while (!serverStop.get()) Thread.sleep(20)
            socket.close()
        }
    }

    @Test
    fun cancellingStopsARequestThatNeverGetsAnAnswer() = runBlocking {
        silentServer()
        val started = System.nanoTime()

        val job = launch(Dispatchers.IO) {
            UrlConnectionTransport().post(url(), mapOf("content-type" to "application/json"), "{}")
        }
        assertTrue(accepted.await(5, TimeUnit.SECONDS))

        withTimeout(5_000) { job.cancelAndJoin() }

        assertTrue(job.isCancelled)
        assertTrue("took too long to stop", System.nanoTime() - started < 10_000_000_000L)
    }

    @Test
    fun aNormalAnswerStillComesThrough() = runBlocking {
        thread(isDaemon = true) {
            server.accept().use { socket ->
                socket.getInputStream().read(ByteArray(4096))
                val body = """{"ok":true}"""
                socket.getOutputStream().write(
                    ("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\n" +
                        "Connection: close\r\n\r\n$body").toByteArray(),
                )
            }
        }

        val response = UrlConnectionTransport().post(url(), emptyMap(), "{}")

        assertEquals(200, response.status)
        assertEquals("""{"ok":true}""", response.body)
    }
}

package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpResponse(val status: Int, val body: String)

/** Cancelling the calling coroutine drops the connection. Callers run it off the main thread. */
fun interface HttpTransport {
    @Throws(IOException::class)
    suspend fun post(url: String, headers: Map<String, String>, body: String): HttpResponse
}

class UrlConnectionTransport : HttpTransport {
    override suspend fun post(url: String, headers: Map<String, String>, body: String): HttpResponse =
        coroutineScope {
            val connection = URL(url).openConnection() as HttpURLConnection
            // Closing the socket is the only way to stop a blocked read.
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    connection.disconnect()
                }
            }
            try {
                exchange(connection, headers, body)
            } catch (e: IOException) {
                ensureActive()
                throw e
            } finally {
                watcher.cancel()
                connection.disconnect()
            }
        }

    private fun exchange(connection: HttpURLConnection, headers: Map<String, String>, body: String): HttpResponse {
        connection.requestMethod = "POST"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.doOutput = true
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return HttpResponse(status, text)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 120_000
    }
}

internal object AnthropicBackend : GeneratorBackend {
    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val API_VERSION = "2026-10-21"

    override val provider = GeneratorProvider.Anthropic

    override fun request(source: String, apiKey: String) = HttpRequest(
        url = ENDPOINT,
        headers = mapOf(
            "content-type" to "application/json",
            "x-api-key" to apiKey,
            "anthropic-version" to API_VERSION,
        ),
        body = GeneratorProtocol.requestBody(source),
    )

    override fun parse(status: Int, body: String): DeckArchive.Deck = GeneratorProtocol.parseResponse(status, body)
}

/** Calls the Anthropic Messages API; plain HTTP, since no library is needed. */
class AnthropicCardGenerator(
    apiKey: () -> String?,
    ioDispatcher: CoroutineDispatcher,
    transport: HttpTransport = UrlConnectionTransport(),
) : CardGenerator by RemoteCardGenerator(AnthropicBackend, apiKey, ioDispatcher, transport)

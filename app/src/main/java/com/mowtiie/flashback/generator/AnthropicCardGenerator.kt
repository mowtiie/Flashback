package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class HttpResponse(val status: Int, val body: String)

/** Blocking; callers run it off the main thread. */
fun interface HttpTransport {
    @Throws(IOException::class)
    fun post(url: String, headers: Map<String, String>, body: String): HttpResponse
}

class UrlConnectionTransport : HttpTransport {
    override fun post(url: String, headers: Map<String, String>, body: String): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
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
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 120_000
    }
}

/** Calls the Anthropic Messages API with the user's own key; plain HTTP, since no library is needed. */
class AnthropicCardGenerator(
    private val apiKey: () -> String?,
    private val ioDispatcher: CoroutineDispatcher,
    private val transport: HttpTransport = UrlConnectionTransport(),
) : CardGenerator {

    override suspend fun generate(source: String): DeckArchive.Deck = withContext(ioDispatcher) {
        val key = apiKey() ?: throw GeneratorException("Add your Anthropic API key in Settings first.")
        val headers = mapOf(
            "content-type" to "application/json",
            "x-api-key" to key,
            "anthropic-version" to API_VERSION,
        )
        val response = try {
            transport.post(ENDPOINT, headers, GeneratorProtocol.requestBody(source))
        } catch (e: IOException) {
            throw GeneratorException("Could not reach Anthropic. Check your connection and try again.", e)
        }
        GeneratorProtocol.parseResponse(response.status, response.body)
    }

    private companion object {
        const val ENDPOINT = "https://api.anthropic.com/v1/messages"
        const val API_VERSION = "2026-10-21"
    }
}

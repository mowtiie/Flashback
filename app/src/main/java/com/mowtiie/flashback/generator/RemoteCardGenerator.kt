package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException

class HttpRequest(val url: String, val headers: Map<String, String>, val body: String)

/** One provider's wire format: how to ask, and how to read the answer. No I/O. */
interface GeneratorBackend {
    val provider: GeneratorProvider

    fun request(request: GeneratorRequest, apiKey: String): HttpRequest

    @Throws(GeneratorException::class)
    fun parse(status: Int, body: String, request: GeneratorRequest): DeckArchive.Deck
}

/** Sends one request to a [GeneratorBackend] with the user's own key. */
class RemoteCardGenerator(
    private val backend: GeneratorBackend,
    private val apiKey: () -> String?,
    private val ioDispatcher: CoroutineDispatcher,
    private val transport: HttpTransport = UrlConnectionTransport(),
) : CardGenerator {

    override suspend fun generate(request: GeneratorRequest): DeckArchive.Deck = withContext(ioDispatcher) {
        val name = backend.provider.displayName
        val key = apiKey() ?: throw GeneratorException("Add your $name API key in Settings first.")
        val http = backend.request(request, key)
        val response = try {
            transport.post(http.url, http.headers, http.body)
        } catch (e: IOException) {
            throw GeneratorException("Could not reach $name. Check your connection and try again.", e)
        }
        backend.parse(response.status, response.body, request)
    }
}

/** Uses whichever provider is selected in Settings when a run starts. */
class SelectedCardGenerator(
    private val preferences: GeneratorPreferences,
    private val generators: Map<GeneratorProvider, CardGenerator>,
) : CardGenerator {
    override suspend fun generate(request: GeneratorRequest): DeckArchive.Deck =
        generators.getValue(preferences.provider).generate(request)
}

package com.mowtiie.flashback.generator

import com.mowtiie.flashback.PreferenceStore

/** The user's own Anthropic API key. Its absence keeps the generator, and the network, switched off. */
class GeneratorPreferences(private val store: PreferenceStore) {

    var apiKey: String?
        get() = store.getString(KEY_API_KEY)?.takeIf { it.isNotEmpty() }
        set(value) = store.putString(KEY_API_KEY, value?.trim()?.ifEmpty { null })

    val hasApiKey: Boolean get() = apiKey != null

    private companion object {
        const val KEY_API_KEY = "generator_api_key"
    }
}

/** The last four characters, enough to tell keys apart without showing the secret. */
fun String.keyHint(): String = "…" + takeLast(4)

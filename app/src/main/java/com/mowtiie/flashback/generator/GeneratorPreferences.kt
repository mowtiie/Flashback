package com.mowtiie.flashback.generator

import com.mowtiie.flashback.PreferenceStore

/** The chosen provider and the user's own key for each. No key keeps the generator, and the network, off. */
class GeneratorPreferences(private val store: PreferenceStore) {

    var provider: GeneratorProvider
        get() = GeneratorProvider.entries.getOrNull(store.getInt(KEY_PROVIDER, 0)) ?: GeneratorProvider.Anthropic
        set(value) = store.putInt(KEY_PROVIDER, value.ordinal)

    /** The key for the selected provider. */
    var apiKey: String?
        get() = apiKey(provider)
        set(value) = setApiKey(provider, value)

    val hasApiKey: Boolean get() = apiKey != null

    fun apiKey(provider: GeneratorProvider): String? =
        store.getString(keyName(provider))?.takeIf { it.isNotEmpty() }

    fun setApiKey(provider: GeneratorProvider, value: String?) =
        store.putString(keyName(provider), value?.trim()?.ifEmpty { null })

    // Anthropic keeps the name it was first saved under.
    private fun keyName(provider: GeneratorProvider) = when (provider) {
        GeneratorProvider.Anthropic -> "generator_api_key"
        GeneratorProvider.Gemini -> "generator_api_key_gemini"
    }

    private companion object {
        const val KEY_PROVIDER = "generator_provider"
    }
}

/** The last four characters, enough to tell keys apart without showing the secret. */
fun String.keyHint(): String = "…" + takeLast(4)

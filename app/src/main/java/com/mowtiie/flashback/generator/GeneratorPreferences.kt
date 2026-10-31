package com.mowtiie.flashback.generator

import com.mowtiie.flashback.PreferenceStore

/** The chosen provider and the user's own key for each. No key keeps the generator, and the network, off. */
class GeneratorPreferences(
    private val store: PreferenceStore,
    private val cipher: SecretCipher = SecretCipher.None,
) {

    var provider: GeneratorProvider
        get() = GeneratorProvider.entries.getOrNull(store.getInt(KEY_PROVIDER, 0)) ?: GeneratorProvider.Anthropic
        set(value) = store.putInt(KEY_PROVIDER, value.ordinal)

    /** The key for the selected provider. */
    var apiKey: String?
        get() = apiKey(provider)
        set(value) = setApiKey(provider, value)

    val hasApiKey: Boolean get() = apiKey != null

    fun apiKey(provider: GeneratorProvider): String? {
        val name = keyName(provider)
        val stored = store.getString(name)?.takeIf { it.isNotEmpty() } ?: return null
        if (stored.startsWith(PREFIX)) return cipher.decrypt(stored.removePrefix(PREFIX))
        setApiKey(provider, stored)
        return stored
    }

    fun setApiKey(provider: GeneratorProvider, value: String?) {
        val clean = value?.trim()?.ifEmpty { null }
        store.putString(keyName(provider), clean?.let { PREFIX + cipher.encrypt(it) })
    }

    // Anthropic keeps the name it was first saved under.
    private fun keyName(provider: GeneratorProvider) = when (provider) {
        GeneratorProvider.Anthropic -> "generator_api_key"
        GeneratorProvider.Gemini -> "generator_api_key_gemini"
    }

    private companion object {
        const val KEY_PROVIDER = "generator_provider"
        const val PREFIX = "enc1:"
    }
}

/** The last four characters, enough to tell keys apart without showing the secret. */
fun String.keyHint(): String = "…" + takeLast(4)

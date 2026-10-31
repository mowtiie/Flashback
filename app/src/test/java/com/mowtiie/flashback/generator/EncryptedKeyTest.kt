package com.mowtiie.flashback.generator

import com.mowtiie.flashback.FakePreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedKeyTest {

    private class ReversingCipher(var broken: Boolean = false) : SecretCipher {
        override fun encrypt(plain: String) = plain.reversed()
        override fun decrypt(encoded: String) = if (broken) null else encoded.reversed()
    }

    private val store = FakePreferenceStore()
    private val cipher = ReversingCipher()
    private val prefs = GeneratorPreferences(store, cipher)

    @Test
    fun theStoredKeyIsNotThePlainKey() {
        prefs.setApiKey(GeneratorProvider.Anthropic, "sk-ant-fakefake0000")

        val stored = store.getString("generator_api_key")
        assertFalse(stored!!.contains("sk-ant-fakefake0000"))
        assertTrue(stored.startsWith("enc1:"))
    }

    @Test
    fun theKeyReadsBackAsEntered() {
        prefs.setApiKey(GeneratorProvider.Gemini, "  AIza-fake-1234 ")
        assertEquals("AIza-fake-1234", prefs.apiKey(GeneratorProvider.Gemini))
    }

    @Test
    fun aKeySavedBeforeEncryptionStillWorksAndGetsEncrypted() {
        store.putString("generator_api_key", "sk-ant-oldplain1111")

        assertEquals("sk-ant-oldplain1111", prefs.apiKey(GeneratorProvider.Anthropic))
        assertTrue(store.getString("generator_api_key")!!.startsWith("enc1:"))
        assertEquals("sk-ant-oldplain1111", prefs.apiKey(GeneratorProvider.Anthropic))
    }

    @Test
    fun aKeyThatCannotBeDecryptedCountsAsNotSet() {
        prefs.setApiKey(GeneratorProvider.Anthropic, "sk-ant-fakefake0000")
        cipher.broken = true

        assertNull(prefs.apiKey(GeneratorProvider.Anthropic))
        assertFalse(prefs.hasApiKey)
    }

    @Test
    fun removingTheKeyClearsIt() {
        prefs.setApiKey(GeneratorProvider.Anthropic, "sk-ant-fakefake0000")
        prefs.setApiKey(GeneratorProvider.Anthropic, null)

        assertNull(store.getString("generator_api_key"))
        assertNull(prefs.apiKey(GeneratorProvider.Anthropic))
    }
}

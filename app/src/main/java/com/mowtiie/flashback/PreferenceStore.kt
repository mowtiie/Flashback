package com.mowtiie.flashback

import android.content.Context
import androidx.core.content.edit

/** The slice of SharedPreferences the app uses, so preference classes can be tested on the JVM. */
interface PreferenceStore {
    fun getInt(key: String, default: Int): Int

    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, default: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)
}

/** The Java app's preferences file, shared so existing settings carry over. */
class SharedPreferenceStore(context: Context) : PreferenceStore {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }

    private companion object {
        const val FILE = "flashback_prefs"
    }
}

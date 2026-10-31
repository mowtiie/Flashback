package com.mowtiie.flashback

import android.content.Context
import androidx.core.content.edit

/** The slice of SharedPreferences the app uses, so preference classes can be tested on the JVM. */
interface PreferenceStore {
    fun getInt(key: String, default: Int): Int

    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, default: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun getString(key: String): String?

    /** A null value removes the entry. */
    fun putString(key: String, value: String?)
}

/** The Java app's preferences file, shared so existing settings carry over. */
class SharedPreferenceStore(context: Context, file: String = FILE) : PreferenceStore {
    private val prefs = context.applicationContext.getSharedPreferences(file, Context.MODE_PRIVATE)

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) = prefs.edit {
        if (value == null) remove(key) else putString(key, value)
    }

    companion object {
        const val FILE = "flashback_prefs"

        /** Excluded from backups, unlike the main file. */
        const val SECRETS_FILE = "flashback_secrets"
    }
}

package com.mowtiie.flashback

class FakePreferenceStore : PreferenceStore {
    val values = mutableMapOf<String, Any>()

    override fun getInt(key: String, default: Int) = values[key] as Int? ?: default

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}

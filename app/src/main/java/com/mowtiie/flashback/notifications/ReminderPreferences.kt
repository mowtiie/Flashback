package com.mowtiie.flashback.notifications

import com.mowtiie.flashback.PreferenceStore

class ReminderPreferences(private val store: PreferenceStore) {

    var enabled: Boolean
        get() = store.getBoolean(KEY_ENABLED, false)
        set(value) = store.putBoolean(KEY_ENABLED, value)

    val hour: Int get() = store.getInt(KEY_HOUR, DEFAULT_HOUR)

    val minute: Int get() = store.getInt(KEY_MINUTE, DEFAULT_MINUTE)

    fun setTime(hour: Int, minute: Int) {
        store.putInt(KEY_HOUR, hour)
        store.putInt(KEY_MINUTE, minute)
    }

    private companion object {
        const val KEY_ENABLED = "reminder_enabled"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
        const val DEFAULT_HOUR = 20
        const val DEFAULT_MINUTE = 0
    }
}

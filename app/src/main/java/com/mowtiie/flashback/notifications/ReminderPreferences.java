package com.mowtiie.flashback.notifications;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The reminder settings, backed by SharedPreferences. Small enough not to need
 * a database table, and kept in the same prefs file the backup rules already
 * name.
 */
public class ReminderPreferences {

    private static final String FILE = "flashback_prefs";
    private static final String KEY_ENABLED = "reminder_enabled";
    private static final String KEY_HOUR = "reminder_hour";
    private static final String KEY_MINUTE = "reminder_minute";

    private static final int DEFAULT_HOUR = 20;
    private static final int DEFAULT_MINUTE = 0;

    private final SharedPreferences prefs;

    public ReminderPreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public int getHour() {
        return prefs.getInt(KEY_HOUR, DEFAULT_HOUR);
    }

    public int getMinute() {
        return prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE);
    }

    public void setTime(int hour, int minute) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply();
    }
}

package com.mowtiie.flashback.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.TimeZone

/** What the settings screen needs from scheduling, so it can be faked in tests. */
fun interface ReminderScheduling {
    /** Sets or cancels the reminder alarm to match the saved preferences, replacing any pending one. */
    fun reschedule()
}

/**
 * Sets one inexact alarm for the next reminder time. It needs no permission and may fire a few
 * minutes late in battery saving. The receiver sets the next one, so the chain continues daily.
 */
class ReminderScheduler(
    context: Context,
    private val prefs: ReminderPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) : ReminderScheduling {
    private val appContext = context.applicationContext

    override fun reschedule() = schedule()

    /** For app start-up and after a reboot or update, which clear alarms; setting the same alarm twice is harmless. */
    fun ensureScheduled() = schedule()

    private fun schedule() {
        val alarms = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent()
        if (!prefs.enabled) {
            alarms.cancel(pending)
            return
        }
        val now = clock()
        val delay = ReminderTime.millisUntilNext(now, prefs.hour, prefs.minute, TimeZone.getDefault())
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now + delay, pending)
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND)
        return PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

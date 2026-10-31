package com.mowtiie.flashback.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** What the settings screen needs from scheduling, so it can be faked in tests. */
fun interface ReminderScheduling {
    /** Enqueues or cancels the reminder to match the saved preferences. */
    fun reschedule()
}

/**
 * Runs the daily reminder as a self-rescheduling one-time worker, so it fires at the
 * exact clock time instead of drifting like periodic work.
 */
class ReminderScheduler(
    context: Context,
    private val prefs: ReminderPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) : ReminderScheduling {
    private val appContext = context.applicationContext

    override fun reschedule() {
        val workManager = WorkManager.getInstance(appContext)
        if (!prefs.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val delay = ReminderTime.millisUntilNext(clock(), prefs.hour, prefs.minute, TimeZone.getDefault())
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        // REPLACE, so a settings change or timezone shift moves the pending job.
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "flashback_daily_reminder"
    }
}

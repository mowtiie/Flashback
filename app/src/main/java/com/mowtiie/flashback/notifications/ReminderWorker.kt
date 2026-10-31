package com.mowtiie.flashback.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mowtiie.flashback.FlashbackApp

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FlashbackApp).container
        try {
            val due = container.repository.countDueEverywhere(System.currentTimeMillis())
            if (due > 0) {
                ReminderNotifier.notifyDue(applicationContext, due)
            }
        } finally {
            // Re-anchor even if today's run failed, or one error ends the daily chain.
            container.reminderScheduler.reschedule()
        }
        return Result.success()
    }
}

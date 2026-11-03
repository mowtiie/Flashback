package com.mowtiie.flashback.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mowtiie.flashback.FlashbackApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The alarm shows the reminder. A reboot, an update or a clock or time zone change clears or
 * shifts the alarm, so those only set it again.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as FlashbackApp).container
        val result = goAsync()
        CoroutineScope(SupervisorJob() + container.dispatchers.io).launch {
            try {
                if (intent.action == ACTION_REMIND) {
                    val due = container.repository.countDueEverywhere(System.currentTimeMillis())
                    if (due > 0) ReminderNotifier.notifyDue(context.applicationContext, due)
                }
            } finally {
                // Set the next alarm even if today's failed, or one error ends the daily chain.
                container.reminderScheduler.reschedule()
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.mowtiie.flashback.action.REMIND"
    }
}

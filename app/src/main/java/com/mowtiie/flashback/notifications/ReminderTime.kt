package com.mowtiie.flashback.notifications

import java.util.Calendar
import java.util.TimeZone

object ReminderTime {

    /** Delay until the next hour:minute; exactly now counts as passed. */
    fun millisUntilNext(now: Long, hour: Int, minute: Int, zone: TimeZone): Long {
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - now
    }
}

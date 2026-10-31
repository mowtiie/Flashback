package com.mowtiie.flashback.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Calendar days (local midnight), not the study rollover hour. */
object StatisticsCalendar {

    fun startOfToday(now: Long, zone: TimeZone): Long = midnight(now, 0, zone).timeInMillis

    fun startOfDaysAgo(now: Long, days: Int, zone: TimeZone): Long =
        midnight(now, days, zone).timeInMillis

    fun todayKey(now: Long, zone: TimeZone): String = dayKey(now, 0, zone)

    /** `yyyy-MM-dd`, matching SQLite's `date(..., 'localtime')`. */
    fun dayKey(now: Long, offset: Int, zone: TimeZone): String {
        val cal = daysAgo(now, offset, zone)
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun dayLabel(now: Long, offset: Int, zone: TimeZone): String =
        daysAgo(now, offset, zone).get(Calendar.DAY_OF_MONTH).toString()

    private fun daysAgo(now: Long, days: Int, zone: TimeZone): Calendar =
        Calendar.getInstance(zone).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -days)
        }

    private fun midnight(now: Long, days: Int, zone: TimeZone): Calendar =
        daysAgo(now, days, zone).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}

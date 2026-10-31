package com.mowtiie.flashback.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderTimeTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(UTC)
        cal.set(year, month, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun targetLaterTodayReturnsShortDelay() {
        assertEquals(HOUR, ReminderTime.millisUntilNext(at(2024, 0, 15, 8, 0), 9, 0, UTC))
    }

    @Test
    fun targetAlreadyPassedRollsToTomorrow() {
        assertEquals(23 * HOUR, ReminderTime.millisUntilNext(at(2024, 0, 15, 10, 0), 9, 0, UTC))
    }

    @Test
    fun exactlyNowSchedulesAFullDayOutNotZero() {
        assertEquals(DAY, ReminderTime.millisUntilNext(at(2024, 0, 15, 9, 0), 9, 0, UTC))
    }

    @Test
    fun minutesAreHonoured() {
        assertEquals(30 * 60_000L, ReminderTime.millisUntilNext(at(2024, 0, 15, 9, 0), 9, 30, UTC))
    }

    @Test
    fun midnightTargetFromLateEvening() {
        assertEquals(HOUR, ReminderTime.millisUntilNext(at(2024, 0, 15, 23, 0), 0, 0, UTC))
    }

    @Test
    fun justPastMidnightTargetRollsToTomorrow() {
        assertEquals(DAY - 60_000L, ReminderTime.millisUntilNext(at(2024, 0, 15, 0, 1), 0, 0, UTC))
    }

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        const val HOUR = 3_600_000L
        const val DAY = 86_400_000L
    }
}

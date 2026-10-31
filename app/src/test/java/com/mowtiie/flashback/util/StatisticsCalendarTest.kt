package com.mowtiie.flashback.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatisticsCalendarTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val berlin = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun startOfTodayIsLocalMidnight() {
        val now = at(utc, 2026, 3, 15, 17, 42)
        assertEquals(at(utc, 2026, 3, 15), StatisticsCalendar.startOfToday(now, utc))
    }

    @Test
    fun startOfTodayUsesTheGivenZoneNotUtc() {
        val now = at(berlin, 2026, 3, 15, 0, 30)
        assertEquals(at(berlin, 2026, 3, 15), StatisticsCalendar.startOfToday(now, berlin))
    }

    @Test
    fun startOfDaysAgoStepsWholeCalendarDays() {
        val now = at(utc, 2026, 3, 15, 17, 42)
        assertEquals(at(utc, 2026, 3, 9), StatisticsCalendar.startOfDaysAgo(now, 6, utc))
    }

    @Test
    fun startOfDaysAgoCrossesMonthAndYear() {
        val now = at(utc, 2026, 1, 2, 9)
        assertEquals(at(utc, 2025, 12, 30), StatisticsCalendar.startOfDaysAgo(now, 3, utc))
    }

    @Test
    fun startOfDaysAgoSurvivesTheSpringForwardDay() {
        val now = at(berlin, 2026, 3, 30, 12)
        val start = StatisticsCalendar.startOfDaysAgo(now, 2, berlin)
        assertEquals(at(berlin, 2026, 3, 28), start)
    }

    @Test
    fun dayKeyIsZeroPaddedIsoDate() {
        val now = at(utc, 2026, 3, 5, 8)
        assertEquals("2026-03-05", StatisticsCalendar.todayKey(now, utc))
        assertEquals("2026-03-04", StatisticsCalendar.dayKey(now, 1, utc))
    }

    @Test
    fun dayKeyCrossesLeapDay() {
        val now = at(utc, 2028, 3, 1, 8)
        assertEquals("2028-02-29", StatisticsCalendar.dayKey(now, 1, utc))
    }

    @Test
    fun dayKeyFollowsTheZone() {
        val now = at(utc, 2026, 3, 15, 23, 30)
        assertEquals("2026-03-15", StatisticsCalendar.todayKey(now, utc))
        assertEquals("2026-03-16", StatisticsCalendar.todayKey(now, berlin))
    }

    @Test
    fun dayLabelIsTheDayOfMonth() {
        val now = at(utc, 2026, 3, 5, 8)
        assertEquals("5", StatisticsCalendar.dayLabel(now, 0, utc))
        assertEquals("28", StatisticsCalendar.dayLabel(now, 5, utc))
    }
}

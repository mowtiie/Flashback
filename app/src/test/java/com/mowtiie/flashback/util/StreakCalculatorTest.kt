package com.mowtiie.flashback.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun threeConsecutiveDaysIncludingToday() {
        assertEquals(
            3,
            StreakCalculator.compute(listOf("2024-03-15", "2024-03-14", "2024-03-13"), "2024-03-15"),
        )
    }

    @Test
    fun streakStaysAliveFromYesterdayBeforeStudyingToday() {
        assertEquals(
            2,
            StreakCalculator.compute(listOf("2024-03-14", "2024-03-13"), "2024-03-15"),
        )
    }

    @Test
    fun gapBreaksTheStreak() {
        assertEquals(
            1,
            StreakCalculator.compute(listOf("2024-03-15", "2024-03-13"), "2024-03-15"),
        )
    }

    @Test
    fun olderThanYesterdayCountsAsBroken() {
        assertEquals(
            0,
            StreakCalculator.compute(listOf("2024-03-10", "2024-03-09"), "2024-03-15"),
        )
    }

    @Test
    fun singleDayToday() {
        assertEquals(1, StreakCalculator.compute(listOf("2024-03-15"), "2024-03-15"))
    }

    @Test
    fun crossesMonthBoundary() {
        assertEquals(
            3,
            StreakCalculator.compute(listOf("2024-03-01", "2024-02-29", "2024-02-28"), "2024-03-01"),
        )
    }

    @Test
    fun crossesYearBoundary() {
        assertEquals(
            2,
            StreakCalculator.compute(listOf("2025-01-01", "2024-12-31"), "2025-01-01"),
        )
    }

    @Test
    fun unorderedInputIsHandled() {
        assertEquals(
            3,
            StreakCalculator.compute(listOf("2024-03-13", "2024-03-15", "2024-03-14"), "2024-03-15"),
        )
    }

    @Test
    fun duplicateDaysDoNotInflate() {
        assertEquals(
            2,
            StreakCalculator.compute(listOf("2024-03-15", "2024-03-15", "2024-03-14"), "2024-03-15"),
        )
    }

    @Test
    fun emptyInputIsZero() {
        assertEquals(0, StreakCalculator.compute(emptyList(), "2024-03-15"))
    }

    @Test
    fun malformedEntriesAreIgnored() {
        assertEquals(1, StreakCalculator.compute(listOf("not-a-date", "2024-03-15"), "2024-03-15"))
    }

    @Test
    fun epochDayAdvancesByOnePerCivilDay() {
        assertEquals(
            1,
            StreakCalculator.epochDay(2024, 3, 1) - StreakCalculator.epochDay(2024, 2, 29),
        )
        assertEquals(
            1,
            StreakCalculator.epochDay(2024, 2, 29) - StreakCalculator.epochDay(2024, 2, 28),
        )
    }
}

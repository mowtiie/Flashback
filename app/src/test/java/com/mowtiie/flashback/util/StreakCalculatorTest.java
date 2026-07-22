package com.mowtiie.flashback.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class StreakCalculatorTest {

    @Test
    public void threeConsecutiveDaysIncludingToday() {
        assertEquals(3, StreakCalculator.compute(
                Arrays.asList("2024-03-15", "2024-03-14", "2024-03-13"), "2024-03-15"));
    }

    @Test
    public void streakStaysAliveFromYesterdayBeforeStudyingToday() {
        assertEquals(2, StreakCalculator.compute(
                Arrays.asList("2024-03-14", "2024-03-13"), "2024-03-15"));
    }

    @Test
    public void gapBreaksTheStreak() {
        assertEquals(1, StreakCalculator.compute(
                Arrays.asList("2024-03-15", "2024-03-13"), "2024-03-15"));
    }

    @Test
    public void olderThanYesterdayCountsAsBroken() {
        assertEquals(0, StreakCalculator.compute(
                Arrays.asList("2024-03-10", "2024-03-09"), "2024-03-15"));
    }

    @Test
    public void singleDayToday() {
        assertEquals(1, StreakCalculator.compute(
                Collections.singletonList("2024-03-15"), "2024-03-15"));
    }

    @Test
    public void crossesMonthBoundary() {
        assertEquals(3, StreakCalculator.compute(
                Arrays.asList("2024-03-01", "2024-02-29", "2024-02-28"), "2024-03-01"));
    }

    @Test
    public void crossesYearBoundary() {
        assertEquals(2, StreakCalculator.compute(
                Arrays.asList("2025-01-01", "2024-12-31"), "2025-01-01"));
    }

    @Test
    public void unorderedInputIsHandled() {
        assertEquals(3, StreakCalculator.compute(
                Arrays.asList("2024-03-13", "2024-03-15", "2024-03-14"), "2024-03-15"));
    }

    @Test
    public void duplicateDaysDoNotInflate() {
        assertEquals(2, StreakCalculator.compute(
                Arrays.asList("2024-03-15", "2024-03-15", "2024-03-14"), "2024-03-15"));
    }

    @Test
    public void emptyInputIsZero() {
        assertEquals(0, StreakCalculator.compute(Collections.emptyList(), "2024-03-15"));
    }

    @Test
    public void malformedEntriesAreIgnored() {
        assertEquals(1, StreakCalculator.compute(
                Arrays.asList("not-a-date", "2024-03-15"), "2024-03-15"));
    }

    @Test
    public void epochDayAdvancesByOnePerCivilDay() {
        assertEquals(1, StreakCalculator.epochDay(2024, 3, 1)
                - StreakCalculator.epochDay(2024, 2, 29));
        assertEquals(1, StreakCalculator.epochDay(2024, 2, 29)
                - StreakCalculator.epochDay(2024, 2, 28));
    }
}

package com.mowtiie.flashback.util

import com.mowtiie.flashback.scheduler.CardState
import com.mowtiie.flashback.scheduler.SchedulingState
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalFormatterTest {

    private fun learning(dueIn: Long, state: Int = CardState.LEARNING) =
        SchedulingState(state = state, dueAt = NOW + dueIn)

    private fun review(days: Int) = SchedulingState(state = CardState.REVIEW, intervalDays = days)

    @Test
    fun learningStepsShowMinutes() {
        assertEquals(Interval.Minutes(10), IntervalFormatter.format(learning(10 * MIN), NOW))
    }

    @Test
    fun learningNeverShowsLessThanOneMinute() {
        assertEquals(Interval.Minutes(1), IntervalFormatter.format(learning(0), NOW))
        assertEquals(Interval.Minutes(1), IntervalFormatter.format(learning(20_000L), NOW))
    }

    @Test
    fun learningAnHourOrMoreShowsRoundedHours() {
        assertEquals(Interval.Hours(1), IntervalFormatter.format(learning(60 * MIN), NOW))
        assertEquals(Interval.Hours(3), IntervalFormatter.format(learning(150 * MIN), NOW))
    }

    @Test
    fun newAndRelearningCardsUseTheirDueTime() {
        assertEquals(
            Interval.Minutes(1),
            IntervalFormatter.format(learning(MIN, CardState.NEW), NOW),
        )
        assertEquals(
            Interval.Minutes(10),
            IntervalFormatter.format(learning(10 * MIN, CardState.RELEARNING), NOW),
        )
    }

    @Test
    fun reviewUnderThirtyDaysShowsDays() {
        assertEquals(Interval.Days(1), IntervalFormatter.format(review(1), NOW))
        assertEquals(Interval.Days(29), IntervalFormatter.format(review(29), NOW))
    }

    @Test
    fun reviewFromThirtyDaysShowsMonths() {
        assertEquals(Interval.Months(30 / 30.4), IntervalFormatter.format(review(30), NOW))
        assertEquals(Interval.Months(364 / 30.4), IntervalFormatter.format(review(364), NOW))
    }

    @Test
    fun reviewFromAYearShowsYears() {
        assertEquals(Interval.Years(1.0), IntervalFormatter.format(review(365), NOW))
        assertEquals(Interval.Years(730 / 365.0), IntervalFormatter.format(review(730), NOW))
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MIN = 60_000L
    }
}

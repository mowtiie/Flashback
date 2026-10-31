package com.mowtiie.flashback.ui.stats

import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.data.model.DailyCount
import com.mowtiie.flashback.repository.StatisticsSource
import com.mowtiie.flashback.util.StatisticsCalendar
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatisticsAssemblerTest {

    private val zone = TimeZone.getTimeZone("UTC")
    private val now = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.MARCH, 15, 17, 0)
    }.timeInMillis

    private class FakeSource : StatisticsSource {
        var total = 0
        var correct = 0
        var timeToday = 0L
        var studyDays = emptyList<String>()
        var daily = emptyList<DailyCount>()
        var breakdown = CardStateBreakdown(0, 0, 0, 0)
        val reviewsSinceCalls = mutableListOf<Long>()
        val reviewsByStart = mutableMapOf<Long, Int>()

        override suspend fun reviewsSince(since: Long): Int {
            reviewsSinceCalls += since
            return reviewsByStart[since] ?: 0
        }

        override suspend fun correctSince(since: Long) = correct

        override suspend fun timeSpentSince(since: Long) = timeToday

        override suspend fun totalReviews() = total

        override suspend fun recentStudyDays(since: Long) = studyDays

        override suspend fun dailyCountsSince(since: Long) = daily

        override suspend fun cardStateBreakdown() = breakdown
    }

    @Test
    fun retentionIsNullWithoutReviewsInTheWindow() = runTest {
        val data = StatisticsAssembler.assemble(FakeSource(), now, zone)
        assertNull(data.retentionPercent)
    }

    @Test
    fun retentionRoundsToWholePercent() = runTest {
        val source = FakeSource().apply {
            reviewsByStart[StatisticsCalendar.startOfDaysAgo(now, 30, zone)] = 3
            correct = 2
        }
        assertEquals(67, StatisticsAssembler.assemble(source, now, zone).retentionPercent)
    }

    @Test
    fun retentionRoundsHalfUp() = runTest {
        val source = FakeSource().apply {
            reviewsByStart[StatisticsCalendar.startOfDaysAgo(now, 30, zone)] = 8
            correct = 5
        }
        assertEquals(63, StatisticsAssembler.assemble(source, now, zone).retentionPercent)
    }

    @Test
    fun todayAndWeekUseCalendarDayStarts() = runTest {
        val source = FakeSource().apply {
            reviewsByStart[StatisticsCalendar.startOfToday(now, zone)] = 5
            reviewsByStart[StatisticsCalendar.startOfDaysAgo(now, 6, zone)] = 40
        }
        val data = StatisticsAssembler.assemble(source, now, zone)
        assertEquals(5, data.reviewsToday)
        assertEquals(40, data.reviewsThisWeek)
    }

    @Test
    fun streakCountsFromTodayWhenStudiedToday() = runTest {
        val source = FakeSource().apply {
            studyDays = listOf("2026-03-15", "2026-03-14", "2026-03-13", "2026-03-10")
        }
        assertEquals(3, StatisticsAssembler.assemble(source, now, zone).streakDays)
    }

    @Test
    fun streakSurvivesUntilTodayIsStudied() = runTest {
        val source = FakeSource().apply { studyDays = listOf("2026-03-14", "2026-03-13") }
        assertEquals(2, StatisticsAssembler.assemble(source, now, zone).streakDays)
    }

    @Test
    fun chartHasFourteenDaysOldestFirstWithZeroFill() = runTest {
        val source = FakeSource().apply {
            daily = listOf(DailyCount("2026-03-15", 7), DailyCount("2026-03-02", 2))
        }
        val days = StatisticsAssembler.assemble(source, now, zone).days
        assertEquals(14, days.size)
        assertEquals(2, days.first().count)
        assertEquals(7, days.last().count)
        assertEquals(9, days.sumOf { it.count })
        assertEquals(0, days[5].count)
    }

    @Test
    fun chartIgnoresDaysOutsideTheWindow() = runTest {
        val source = FakeSource().apply { daily = listOf(DailyCount("2026-03-01", 9)) }
        assertEquals(0, StatisticsAssembler.assemble(source, now, zone).days.sumOf { it.count })
    }

    @Test
    fun chartLabelsTheEndsAndEveryThirdDay() = runTest {
        val labels = StatisticsAssembler.assemble(FakeSource(), now, zone).days.map { it.label }
        assertEquals(
            listOf("2", "3", "", "", "6", "", "", "9", "", "", "12", "", "", "15"),
            labels,
        )
    }

    @Test
    fun timeSpentIsMinutesBelowAnHour() {
        assertEquals(TimeSpent.Minutes(0), StatisticsAssembler.timeSpent(59_999L))
        assertEquals(TimeSpent.Minutes(59), StatisticsAssembler.timeSpent(59 * 60_000L + 59_000L))
    }

    @Test
    fun timeSpentIsFractionalHoursFromAnHour() {
        assertEquals(TimeSpent.Hours(1.0), StatisticsAssembler.timeSpent(60 * 60_000L))
        assertEquals(TimeSpent.Hours(1.5), StatisticsAssembler.timeSpent(90 * 60_000L))
    }

    @Test
    fun activityComesFromReviewsOrCards() = runTest {
        assertFalse(StatisticsAssembler.assemble(FakeSource(), now, zone).hasAnyActivity)

        val reviewed = FakeSource().apply { total = 1 }
        assertTrue(StatisticsAssembler.assemble(reviewed, now, zone).hasAnyActivity)

        val cardsOnly = FakeSource().apply { breakdown = CardStateBreakdown(2, 0, 0, 0) }
        assertTrue(StatisticsAssembler.assemble(cardsOnly, now, zone).hasAnyActivity)
    }
}

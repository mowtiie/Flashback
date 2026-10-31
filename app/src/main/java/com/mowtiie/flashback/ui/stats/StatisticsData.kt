package com.mowtiie.flashback.ui.stats

import androidx.compose.runtime.Immutable
import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.repository.StatisticsSource
import com.mowtiie.flashback.util.StatisticsCalendar
import com.mowtiie.flashback.util.StreakCalculator
import java.util.TimeZone
import kotlin.math.roundToInt

/** One day of the activity chart; the label is empty when it would collide with its neighbours. */
@Immutable
data class DayBar(val count: Int, val label: String)

sealed interface TimeSpent {
    data class Minutes(val value: Long) : TimeSpent

    data class Hours(val value: Double) : TimeSpent
}

@Immutable
data class StatisticsData(
    val reviewsToday: Int = 0,
    val reviewsThisWeek: Int = 0,
    val totalReviews: Int = 0,
    val streakDays: Int = 0,
    /** Null when there were no reviews in the retention window. */
    val retentionPercent: Int? = null,
    val timeToday: TimeSpent = TimeSpent.Minutes(0),
    val breakdown: CardStateBreakdown = CardStateBreakdown(0, 0, 0, 0),
    val days: List<DayBar> = emptyList(),
) {
    val hasAnyActivity: Boolean get() = totalReviews > 0 || breakdown.total() > 0
}

object StatisticsAssembler {
    const val CHART_DAYS = 14
    private const val RETENTION_WINDOW_DAYS = 30
    private const val STREAK_WINDOW_DAYS = 400
    private const val LABEL_EVERY = 3
    private const val MINUTE_MS = 60_000L
    private const val MINUTES_PER_HOUR = 60

    suspend fun assemble(source: StatisticsSource, now: Long, zone: TimeZone): StatisticsData {
        val startToday = StatisticsCalendar.startOfToday(now, zone)
        val startWeek = StatisticsCalendar.startOfDaysAgo(now, 6, zone)
        val startRetention = StatisticsCalendar.startOfDaysAgo(now, RETENTION_WINDOW_DAYS, zone)
        val startChart = StatisticsCalendar.startOfDaysAgo(now, CHART_DAYS - 1, zone)
        val startStreak = StatisticsCalendar.startOfDaysAgo(now, STREAK_WINDOW_DAYS, zone)

        val windowReviews = source.reviewsSince(startRetention)
        val retention = if (windowReviews > 0) {
            (source.correctSince(startRetention) * 100f / windowReviews).roundToInt()
        } else {
            null
        }

        return StatisticsData(
            reviewsToday = source.reviewsSince(startToday),
            reviewsThisWeek = source.reviewsSince(startWeek),
            totalReviews = source.totalReviews(),
            streakDays = StreakCalculator.compute(
                source.recentStudyDays(startStreak),
                StatisticsCalendar.todayKey(now, zone),
            ),
            retentionPercent = retention,
            timeToday = timeSpent(source.timeSpentSince(startToday)),
            breakdown = source.cardStateBreakdown(),
            days = chartDays(source, now, zone, startChart),
        )
    }

    fun timeSpent(millis: Long): TimeSpent {
        val minutes = millis / MINUTE_MS
        return if (minutes < MINUTES_PER_HOUR) {
            TimeSpent.Minutes(minutes)
        } else {
            TimeSpent.Hours(minutes / MINUTES_PER_HOUR.toDouble())
        }
    }

    private suspend fun chartDays(
        source: StatisticsSource,
        now: Long,
        zone: TimeZone,
        since: Long,
    ): List<DayBar> {
        val byDay = source.dailyCountsSince(since).associate { it.day to it.count }
        return List(CHART_DAYS) { index ->
            val offset = CHART_DAYS - 1 - index
            val labelled = index == 0 || index == CHART_DAYS - 1 || offset % LABEL_EVERY == 0
            DayBar(
                count = byDay[StatisticsCalendar.dayKey(now, offset, zone)] ?: 0,
                label = if (labelled) StatisticsCalendar.dayLabel(now, offset, zone) else "",
            )
        }
    }
}

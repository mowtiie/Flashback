package com.mowtiie.flashback.util

import com.mowtiie.flashback.scheduler.CardState
import com.mowtiie.flashback.scheduler.SchedulingState

/** Android-free, so the UI only has to pick the string for each unit. */
sealed interface Interval {
    data class Minutes(val value: Long) : Interval
    data class Hours(val value: Long) : Interval
    data class Days(val value: Int) : Interval
    data class Months(val value: Double) : Interval
    data class Years(val value: Double) : Interval
}

object IntervalFormatter {

    private const val MINUTE_MS = 60_000L

    fun format(state: SchedulingState, now: Long): Interval {
        if (CardState.isLearningLike(state.state)) {
            val minutes = maxOf(1L, (state.dueAt - now) / MINUTE_MS)
            return if (minutes < 60) {
                Interval.Minutes(minutes)
            } else {
                Interval.Hours(Math.round(minutes / 60.0))
            }
        }

        val days = state.intervalDays
        return when {
            days < 30 -> Interval.Days(days)
            days < 365 -> Interval.Months(days / 30.4)
            else -> Interval.Years(days / 365.0)
        }
    }
}

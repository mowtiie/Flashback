package com.mowtiie.flashback.scheduler

import java.util.Calendar
import java.util.Random
import java.util.TimeZone

/** SM-2 in Anki's four-button form. Not thread safe (holds a [Random]). */
class Sm2Scheduler(
    val config: SchedulerConfig = SchedulerConfig(),
    private val random: Random = Random(),
    private val timeZone: TimeZone = TimeZone.getDefault(),
) {
    private var suppressFuzz = false

    /** As [answer] without fuzz, for button labels. */
    fun preview(current: SchedulingState, rating: Rating, now: Long): SchedulingState {
        suppressFuzz = true
        try {
            return answer(current, rating, now)
        } finally {
            suppressFuzz = false
        }
    }

    /** Returns the new state; the input is untouched so callers can keep it for undo. */
    fun answer(current: SchedulingState, rating: Rating, now: Long): SchedulingState {
        val next = current.copy(reps = current.reps + 1, lastReviewedAt = now)

        when (current.state) {
            CardState.NEW, CardState.LEARNING ->
                advanceThroughSteps(next, rating, now, config.learningStepsMinutes, false)
            CardState.RELEARNING ->
                advanceThroughSteps(next, rating, now, config.relearningStepsMinutes, true)
            CardState.REVIEW -> answerReviewCard(next, rating, now)
            else -> throw IllegalStateException("Unknown card state: ${current.state}")
        }
        return next
    }

    private fun advanceThroughSteps(
        s: SchedulingState,
        rating: Rating,
        now: Long,
        steps: IntArray,
        relearning: Boolean,
    ) {
        val phase = if (relearning) CardState.RELEARNING else CardState.LEARNING

        when (rating) {
            Rating.AGAIN -> {
                s.state = phase
                s.learningStep = 0
                s.dueAt = now + steps[0] * MINUTE_MS
            }

            Rating.HARD -> {
                s.state = phase
                s.learningStep = minOf(s.learningStep, steps.size - 1)
                s.dueAt = now + steps[s.learningStep] * MINUTE_MS
            }

            Rating.GOOD -> {
                val nextStep = s.learningStep + 1
                if (nextStep >= steps.size) {
                    graduate(
                        s, now,
                        if (relearning) maxOf(config.minimumIntervalDays, s.intervalDays)
                        else config.graduatingIntervalDays,
                    )
                } else {
                    s.state = phase
                    s.learningStep = nextStep
                    s.dueAt = now + steps[nextStep] * MINUTE_MS
                }
            }

            Rating.EASY -> graduate(
                s, now,
                if (relearning) maxOf(config.minimumIntervalDays, s.intervalDays)
                else config.easyIntervalDays,
            )
        }
    }

    private fun graduate(s: SchedulingState, now: Long, intervalDays: Int) {
        s.state = CardState.REVIEW
        s.learningStep = 0
        s.intervalDays = clampInterval(intervalDays)
        s.dueAt = dueAfterDays(now, s.intervalDays)
    }

    private fun answerReviewCard(s: SchedulingState, rating: Rating, now: Long) {
        if (rating == Rating.AGAIN) {
            s.lapses += 1
            s.easeFactor = clampEase(s.easeFactor + config.againEaseDelta)
            s.intervalDays = clampInterval(
                Math.round(s.intervalDays * config.lapseIntervalMultiplier).toInt(),
            )
            s.state = CardState.RELEARNING
            s.learningStep = 0
            s.dueAt = now + config.relearningStepsMinutes[0] * MINUTE_MS
            return
        }

        val previous = maxOf(s.intervalDays, config.minimumIntervalDays)
        var grown = when (rating) {
            Rating.HARD -> {
                s.easeFactor = clampEase(s.easeFactor + config.hardEaseDelta)
                Math.round(previous * config.hardIntervalMultiplier).toInt()
            }

            Rating.GOOD -> Math.round(previous * s.easeFactor).toInt()

            Rating.EASY -> {
                s.easeFactor = clampEase(s.easeFactor + config.easyEaseDelta)
                Math.round(previous * s.easeFactor * config.easyBonus).toInt()
            }

            Rating.AGAIN -> throw IllegalStateException("Unreachable rating: $rating")
        }

        // Fuzz first, then guarantee at least a day of growth, or low-ease cards can stall.
        s.intervalDays = clampInterval(maxOf(applyFuzz(grown), previous + 1))
        s.state = CardState.REVIEW
        s.dueAt = dueAfterDays(now, s.intervalDays)
    }

    private fun applyFuzz(intervalDays: Int): Int {
        if (!config.fuzzEnabled || suppressFuzz || intervalDays < 3) {
            return intervalDays
        }
        val spread = maxOf(1, Math.round(intervalDays * 0.05).toInt())
        return intervalDays + random.nextInt(spread * 2 + 1) - spread
    }

    private fun clampEase(ease: Double): Double = maxOf(config.minimumEase, ease)

    private fun clampInterval(days: Int): Int =
        minOf(config.maximumIntervalDays, maxOf(config.minimumIntervalDays, days))

    /** Day-granularity due dates snap to the rollover hour. */
    internal fun dueAfterDays(now: Long, days: Int): Long {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = now + days * DAY_MS
        cal.set(Calendar.HOUR_OF_DAY, config.rolloverHour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val DAY_MS = 86_400_000L
    }
}

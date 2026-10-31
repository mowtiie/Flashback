package com.mowtiie.flashback.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Random
import java.util.TimeZone

/** Fuzz is disabled so intervals are exact. */
class Sm2SchedulerTest {

    private lateinit var config: SchedulerConfig
    private lateinit var scheduler: Sm2Scheduler

    @Before
    fun setUp() {
        config = SchedulerConfig()
        config.fuzzEnabled = false
        scheduler = Sm2Scheduler(config, Random(1), TimeZone.getTimeZone("UTC"))
    }

    private fun newCard() = SchedulingState(state = CardState.NEW, easeFactor = config.startingEase)

    private fun reviewCard(intervalDays: Int, ease: Double) = SchedulingState(
        state = CardState.REVIEW,
        intervalDays = intervalDays,
        easeFactor = ease,
        reps = 5,
    )

    @Test
    fun newCardGoodEntersFirstLearningStep() {
        val result = scheduler.answer(newCard(), Rating.GOOD, NOW)

        assertEquals(CardState.LEARNING, result.state)
        assertEquals(1, result.learningStep)
        assertEquals(NOW + 10 * MINUTE, result.dueAt)
        assertEquals(1, result.reps)
    }

    @Test
    fun newCardEasyGraduatesImmediately() {
        val result = scheduler.answer(newCard(), Rating.EASY, NOW)

        assertEquals(CardState.REVIEW, result.state)
        assertEquals(config.easyIntervalDays, result.intervalDays)
    }

    @Test
    fun finalLearningStepGraduatesToOneDay() {
        val onLastStep = newCard().apply {
            state = CardState.LEARNING
            learningStep = 1
        }

        val result = scheduler.answer(onLastStep, Rating.GOOD, NOW)

        assertEquals(CardState.REVIEW, result.state)
        assertEquals(config.graduatingIntervalDays, result.intervalDays)
    }

    @Test
    fun againResetsLearningToFirstStep() {
        val onLastStep = newCard().apply {
            state = CardState.LEARNING
            learningStep = 1
        }

        val result = scheduler.answer(onLastStep, Rating.AGAIN, NOW)

        assertEquals(CardState.LEARNING, result.state)
        assertEquals(0, result.learningStep)
        assertEquals(NOW + MINUTE, result.dueAt)
    }

    @Test
    fun goodOnReviewMultipliesByEase() {
        val result = scheduler.answer(reviewCard(10, 2.5), Rating.GOOD, NOW)

        assertEquals(25, result.intervalDays)
        assertEquals(2.5, result.easeFactor, 0.0001)
    }

    @Test
    fun hardOnReviewGrowsSlowlyAndLowersEase() {
        val result = scheduler.answer(reviewCard(10, 2.5), Rating.HARD, NOW)

        assertEquals(12, result.intervalDays)
        assertEquals(2.35, result.easeFactor, 0.0001)
    }

    @Test
    fun easyOnReviewAppliesBonusAndRaisesEase() {
        val result = scheduler.answer(reviewCard(10, 2.5), Rating.EASY, NOW)

        assertEquals(2.65, result.easeFactor, 0.0001)
        assertEquals(34, result.intervalDays)
    }

    @Test
    fun lapseSendsCardToRelearningAndCountsIt() {
        val result = scheduler.answer(reviewCard(30, 2.5), Rating.AGAIN, NOW)

        assertEquals(CardState.RELEARNING, result.state)
        assertEquals(1, result.lapses)
        assertEquals(2.3, result.easeFactor, 0.0001)
        assertEquals(config.minimumIntervalDays, result.intervalDays)
        assertEquals(NOW + 10 * MINUTE, result.dueAt)
    }

    @Test
    fun relearningGoodReturnsCardToReview() {
        val lapsed = scheduler.answer(reviewCard(30, 2.5), Rating.AGAIN, NOW)

        val result = scheduler.answer(lapsed, Rating.GOOD, NOW + 10 * MINUTE)

        assertEquals(CardState.REVIEW, result.state)
        assertTrue(result.intervalDays >= config.minimumIntervalDays)
    }

    @Test
    fun easeNeverFallsBelowFloor() {
        val s = reviewCard(5, 1.3)

        val result = scheduler.answer(s, Rating.HARD, NOW)

        assertEquals(config.minimumEase, result.easeFactor, 0.0001)
    }

    @Test
    fun intervalAlwaysAdvancesEvenAtMinimumEase() {
        val result = scheduler.answer(reviewCard(1, 1.3), Rating.GOOD, NOW)

        assertTrue("interval must grow", result.intervalDays > 1)
    }

    @Test
    fun fuzzNeverTakesTheIntervalBackBelowTheMinimumGrowth() {
        val fuzzed = SchedulerConfig()
        for (seed in 0L until 300L) {
            val result = Sm2Scheduler(fuzzed, Random(seed), TimeZone.getTimeZone("UTC"))
                .answer(reviewCard(2, 1.3), Rating.GOOD, NOW)

            assertTrue("seed $seed gave ${result.intervalDays}", result.intervalDays >= 3)
        }
    }

    @Test
    fun intervalIsCappedAtMaximum() {
        config.maximumIntervalDays = 365

        val result = scheduler.answer(reviewCard(300, 2.5), Rating.EASY, NOW)

        assertEquals(365, result.intervalDays)
    }

    @Test
    fun dueDateSnapsToRolloverHour() {
        val due = scheduler.dueAfterDays(NOW, 1)

        assertTrue(due > NOW)
        val hourOfDay = (due / 3_600_000L) % 24L
        assertEquals(config.rolloverHour, hourOfDay.toInt())
    }

    @Test
    fun answerLeavesInputUntouchedSoUndoCanUseIt() {
        val before = reviewCard(10, 2.5)

        scheduler.answer(before, Rating.EASY, NOW)

        assertEquals(10, before.intervalDays)
        assertEquals(2.5, before.easeFactor, 0.0001)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val NOW = 1_700_000_000_000L
    }
}

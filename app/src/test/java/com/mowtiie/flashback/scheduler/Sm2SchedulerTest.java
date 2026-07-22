package com.mowtiie.flashback.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;
import java.util.TimeZone;

/**
 * Pure JVM tests: no Robolectric, no emulator. Fuzz is disabled so intervals
 * are exact.
 */
public class Sm2SchedulerTest {

    private static final long MINUTE = 60_000L;
    private static final long NOW = 1_700_000_000_000L;

    private SchedulerConfig config;
    private Sm2Scheduler scheduler;

    @Before
    public void setUp() {
        config = new SchedulerConfig();
        config.fuzzEnabled = false;
        scheduler = new Sm2Scheduler(config, new Random(1), TimeZone.getTimeZone("UTC"));
    }

    private SchedulingState newCard() {
        SchedulingState s = new SchedulingState();
        s.state = CardState.NEW;
        s.easeFactor = config.startingEase;
        return s;
    }

    private SchedulingState reviewCard(int intervalDays, double ease) {
        SchedulingState s = new SchedulingState();
        s.state = CardState.REVIEW;
        s.intervalDays = intervalDays;
        s.easeFactor = ease;
        s.reps = 5;
        return s;
    }

    @Test
    public void newCardGoodEntersFirstLearningStep() {
        SchedulingState result = scheduler.answer(newCard(), Rating.GOOD, NOW);

        assertEquals(CardState.LEARNING, result.state);
        assertEquals(1, result.learningStep);
        assertEquals(NOW + 10 * MINUTE, result.dueAt);
        assertEquals(1, result.reps);
    }

    @Test
    public void newCardEasyGraduatesImmediately() {
        SchedulingState result = scheduler.answer(newCard(), Rating.EASY, NOW);

        assertEquals(CardState.REVIEW, result.state);
        assertEquals(config.easyIntervalDays, result.intervalDays);
    }

    @Test
    public void finalLearningStepGraduatesToOneDay() {
        SchedulingState onLastStep = newCard();
        onLastStep.state = CardState.LEARNING;
        onLastStep.learningStep = 1;

        SchedulingState result = scheduler.answer(onLastStep, Rating.GOOD, NOW);

        assertEquals(CardState.REVIEW, result.state);
        assertEquals(config.graduatingIntervalDays, result.intervalDays);
    }

    @Test
    public void againResetsLearningToFirstStep() {
        SchedulingState onLastStep = newCard();
        onLastStep.state = CardState.LEARNING;
        onLastStep.learningStep = 1;

        SchedulingState result = scheduler.answer(onLastStep, Rating.AGAIN, NOW);

        assertEquals(CardState.LEARNING, result.state);
        assertEquals(0, result.learningStep);
        assertEquals(NOW + MINUTE, result.dueAt);
    }

    @Test
    public void goodOnReviewMultipliesByEase() {
        SchedulingState result = scheduler.answer(reviewCard(10, 2.5d), Rating.GOOD, NOW);

        assertEquals(25, result.intervalDays);
        assertEquals(2.5d, result.easeFactor, 0.0001d);
    }

    @Test
    public void hardOnReviewGrowsSlowlyAndLowersEase() {
        SchedulingState result = scheduler.answer(reviewCard(10, 2.5d), Rating.HARD, NOW);

        assertEquals(12, result.intervalDays);
        assertEquals(2.35d, result.easeFactor, 0.0001d);
    }

    @Test
    public void easyOnReviewAppliesBonusAndRaisesEase() {
        SchedulingState result = scheduler.answer(reviewCard(10, 2.5d), Rating.EASY, NOW);

        assertEquals(2.65d, result.easeFactor, 0.0001d);
        assertEquals(34, result.intervalDays);
    }

    @Test
    public void lapseSendsCardToRelearningAndCountsIt() {
        SchedulingState result = scheduler.answer(reviewCard(30, 2.5d), Rating.AGAIN, NOW);

        assertEquals(CardState.RELEARNING, result.state);
        assertEquals(1, result.lapses);
        assertEquals(2.3d, result.easeFactor, 0.0001d);
        assertEquals(config.minimumIntervalDays, result.intervalDays);
        assertEquals(NOW + 10 * MINUTE, result.dueAt);
    }

    @Test
    public void relearningGoodReturnsCardToReview() {
        SchedulingState lapsed = scheduler.answer(reviewCard(30, 2.5d), Rating.AGAIN, NOW);

        SchedulingState result = scheduler.answer(lapsed, Rating.GOOD, NOW + 10 * MINUTE);

        assertEquals(CardState.REVIEW, result.state);
        assertTrue(result.intervalDays >= config.minimumIntervalDays);
    }

    @Test
    public void easeNeverFallsBelowFloor() {
        SchedulingState s = reviewCard(5, 1.3d);

        SchedulingState result = scheduler.answer(s, Rating.HARD, NOW);

        assertEquals(config.minimumEase, result.easeFactor, 0.0001d);
    }

    @Test
    public void intervalAlwaysAdvancesEvenAtMinimumEase() {
        SchedulingState result = scheduler.answer(reviewCard(1, 1.3d), Rating.GOOD, NOW);

        assertTrue("interval must grow", result.intervalDays > 1);
    }

    @Test
    public void intervalIsCappedAtMaximum() {
        config.maximumIntervalDays = 365;

        SchedulingState result = scheduler.answer(reviewCard(300, 2.5d), Rating.EASY, NOW);

        assertEquals(365, result.intervalDays);
    }

    @Test
    public void dueDateSnapsToRolloverHour() {
        long due = scheduler.dueAfterDays(NOW, 1);

        assertTrue(due > NOW);
        long hourOfDay = (due / 3_600_000L) % 24L;
        assertEquals(config.rolloverHour, (int) hourOfDay);
    }

    @Test
    public void answerLeavesInputUntouchedSoUndoCanUseIt() {
        SchedulingState before = reviewCard(10, 2.5d);

        scheduler.answer(before, Rating.EASY, NOW);

        assertEquals(10, before.intervalDays);
        assertEquals(2.5d, before.easeFactor, 0.0001d);
    }
}

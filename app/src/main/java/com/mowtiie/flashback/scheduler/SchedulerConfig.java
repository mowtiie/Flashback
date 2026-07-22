package com.mowtiie.flashback.scheduler;

/**
 * Tunable knobs for {@link Sm2Scheduler}. Defaults mirror Anki's SM-2 preset.
 * Phase 2 can surface a subset of these in Settings.
 */
public class SchedulerConfig {

    /** Delays, in minutes, for each learning step of a new card. */
    public int[] learningStepsMinutes = {1, 10};

    /** Delays, in minutes, applied after a review card lapses. */
    public int[] relearningStepsMinutes = {10};

    /** Interval given when a card graduates with GOOD. */
    public int graduatingIntervalDays = 1;

    /** Interval given when a learning card is answered EASY. */
    public int easyIntervalDays = 4;

    /** Multiplier applied to the old interval when a review card lapses. */
    public double lapseIntervalMultiplier = 0.0d;

    public int minimumIntervalDays = 1;

    public int maximumIntervalDays = 36500;

    public double startingEase = 2.5d;

    public double minimumEase = 1.3d;

    public double hardIntervalMultiplier = 1.2d;

    public double easyBonus = 1.3d;

    public double againEaseDelta = -0.20d;
    public double hardEaseDelta = -0.15d;
    public double easyEaseDelta = 0.15d;

    /**
     * Spreads due dates slightly so a batch of cards added together does not
     * come back as a single wall months later. Disabled in tests.
     */
    public boolean fuzzEnabled = true;

    /** Hour of day (0-23) at which a new study day begins. */
    public int rolloverHour = 4;
}

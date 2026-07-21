package com.mowtiie.flashback.scheduler;

import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

public class Sm2Scheduler {

    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 86_400_000L;

    private final SchedulerConfig config;
    private final Random random;
    private final TimeZone timeZone;

    private boolean suppressFuzz;

    public Sm2Scheduler() {
        this(new SchedulerConfig());
    }

    public Sm2Scheduler(SchedulerConfig config) {
        this(config, new Random(), TimeZone.getDefault());
    }

    public Sm2Scheduler(SchedulerConfig config, Random random, TimeZone timeZone) {
        this.config = config;
        this.random = random;
        this.timeZone = timeZone;
    }

    public SchedulerConfig getConfig() {
        return config;
    }

    public SchedulingState preview(SchedulingState current, Rating rating, long now) {
        suppressFuzz = true;
        try {
            return answer(current, rating, now);
        } finally {
            suppressFuzz = false;
        }
    }

    public SchedulingState answer(SchedulingState current, Rating rating, long now) {
        SchedulingState next = current.copy();
        next.reps = current.reps + 1;
        next.lastReviewedAt = now;

        switch (current.state) {
            case CardState.NEW:
            case CardState.LEARNING:
                advanceThroughSteps(next, rating, now, config.learningStepsMinutes, false);
                break;
            case CardState.RELEARNING:
                advanceThroughSteps(next, rating, now, config.relearningStepsMinutes, true);
                break;
            case CardState.REVIEW:
                answerReviewCard(next, rating, now);
                break;
            default:
                throw new IllegalStateException("Unknown card state: " + current.state);
        }
        return next;
    }

    private void advanceThroughSteps(SchedulingState s, Rating rating, long now,
                                     int[] steps, boolean relearning) {
        int phase = relearning ? CardState.RELEARNING : CardState.LEARNING;

        switch (rating) {
            case AGAIN:
                s.state = phase;
                s.learningStep = 0;
                s.dueAt = now + steps[0] * MINUTE_MS;
                break;

            case HARD:
                s.state = phase;
                s.learningStep = Math.min(s.learningStep, steps.length - 1);
                s.dueAt = now + steps[s.learningStep] * MINUTE_MS;
                break;

            case GOOD:
                int nextStep = s.learningStep + 1;
                if (nextStep >= steps.length) {
                    graduate(s, now, relearning
                            ? Math.max(config.minimumIntervalDays, s.intervalDays)
                            : config.graduatingIntervalDays);
                } else {
                    s.state = phase;
                    s.learningStep = nextStep;
                    s.dueAt = now + steps[nextStep] * MINUTE_MS;
                }
                break;

            case EASY:
                graduate(s, now, relearning
                        ? Math.max(config.minimumIntervalDays, s.intervalDays)
                        : config.easyIntervalDays);
                break;
        }
    }

    private void graduate(SchedulingState s, long now, int intervalDays) {
        s.state = CardState.REVIEW;
        s.learningStep = 0;
        s.intervalDays = clampInterval(intervalDays);
        s.dueAt = dueAfterDays(now, s.intervalDays);
    }

    private void answerReviewCard(SchedulingState s, Rating rating, long now) {
        if (rating == Rating.AGAIN) {
            s.lapses = s.lapses + 1;
            s.easeFactor = clampEase(s.easeFactor + config.againEaseDelta);
            s.intervalDays = clampInterval(
                    (int) Math.round(s.intervalDays * config.lapseIntervalMultiplier));
            s.state = CardState.RELEARNING;
            s.learningStep = 0;
            s.dueAt = now + config.relearningStepsMinutes[0] * MINUTE_MS;
            return;
        }

        int previous = Math.max(s.intervalDays, config.minimumIntervalDays);
        int grown;

        switch (rating) {
            case HARD:
                s.easeFactor = clampEase(s.easeFactor + config.hardEaseDelta);
                grown = (int) Math.round(previous * config.hardIntervalMultiplier);
                break;
            case GOOD:
                grown = (int) Math.round(previous * s.easeFactor);
                break;
            case EASY:
                s.easeFactor = clampEase(s.easeFactor + config.easyEaseDelta);
                grown = (int) Math.round(previous * s.easeFactor * config.easyBonus);
                break;
            default:
                throw new IllegalStateException("Unreachable rating: " + rating);
        }

        grown = Math.max(grown, previous + 1);
        s.intervalDays = clampInterval(applyFuzz(grown));
        s.state = CardState.REVIEW;
        s.dueAt = dueAfterDays(now, s.intervalDays);
    }

    private int applyFuzz(int intervalDays) {
        if (!config.fuzzEnabled || suppressFuzz || intervalDays < 3) {
            return intervalDays;
        }
        int spread = Math.max(1, (int) Math.round(intervalDays * 0.05d));
        return intervalDays + random.nextInt(spread * 2 + 1) - spread;
    }

    private double clampEase(double ease) {
        return Math.max(config.minimumEase, ease);
    }

    private int clampInterval(int days) {
        return Math.min(config.maximumIntervalDays,
                Math.max(config.minimumIntervalDays, days));
    }

    long dueAfterDays(long now, int days) {
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(now + days * DAY_MS);
        cal.set(Calendar.HOUR_OF_DAY, config.rolloverHour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis();
    }
}

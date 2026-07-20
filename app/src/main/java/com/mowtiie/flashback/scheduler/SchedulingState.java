package com.mowtiie.flashback.scheduler;

public class SchedulingState {

    public int state = CardState.NEW;
    public int learningStep;
    public int intervalDays;
    public double easeFactor = 2.5d;
    public int reps;
    public int lapses;
    public long dueAt;
    public long lastReviewedAt;

    public SchedulingState copy() {
        SchedulingState s = new SchedulingState();
        s.state = state;
        s.learningStep = learningStep;
        s.intervalDays = intervalDays;
        s.easeFactor = easeFactor;
        s.reps = reps;
        s.lapses = lapses;
        s.dueAt = dueAt;
        s.lastReviewedAt = lastReviewedAt;
        return s;
    }

    @Override
    public String toString() {
        return "SchedulingState{state=" + state
                + ", step=" + learningStep
                + ", interval=" + intervalDays
                + ", ease=" + easeFactor
                + ", reps=" + reps
                + ", lapses=" + lapses
                + '}';
    }
}

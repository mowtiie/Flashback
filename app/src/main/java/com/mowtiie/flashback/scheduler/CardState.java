package com.mowtiie.flashback.scheduler;

/** Lifecycle phases of a card. Stored as an int so Room needs no converter. */
public final class CardState {

    /** Never studied. */
    public static final int NEW = 0;

    /** Working through the initial learning steps. */
    public static final int LEARNING = 1;

    /** Graduated; scheduled in days. */
    public static final int REVIEW = 2;

    /** Was in REVIEW, then failed. Working back through the relearning steps. */
    public static final int RELEARNING = 3;

    private CardState() {
    }

    public static boolean isLearningLike(int state) {
        return state == NEW || state == LEARNING || state == RELEARNING;
    }
}

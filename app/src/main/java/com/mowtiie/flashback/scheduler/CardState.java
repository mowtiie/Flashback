package com.mowtiie.flashback.scheduler;

public final class CardState {

    public static final int NEW = 0;

    public static final int LEARNING = 1;

    public static final int REVIEW = 2;

    public static final int RELEARNING = 3;

    private CardState() {
    }

    public static boolean isLearningLike(int state) {
        return state == NEW || state == LEARNING || state == RELEARNING;
    }
}

package com.mowtiie.flashback.scheduler;

/** The four grading buttons shown after a card is revealed. */
public enum Rating {

    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    public final int value;

    Rating(int value) {
        this.value = value;
    }

    public static Rating fromValue(int value) {
        for (Rating r : values()) {
            if (r.value == value) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown rating value: " + value);
    }
}

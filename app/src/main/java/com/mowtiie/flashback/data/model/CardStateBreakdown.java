package com.mowtiie.flashback.data.model;

/**
 * Collection-wide card counts by maturity, using the conventional flashcard
 * buckets. "Young" and "mature" split review cards at a 21-day interval, the
 * long-standing threshold for a card being considered well-retained.
 */
public class CardStateBreakdown {

    public int newCount;
    public int learningCount;
    public int youngCount;
    public int matureCount;

    public int total() {
        return newCount + learningCount + youngCount + matureCount;
    }
}

package com.mowtiie.flashback.ui.stats;

import com.mowtiie.flashback.data.model.CardStateBreakdown;

/**
 * A single consistent snapshot of the statistics screen, assembled off the main
 * thread so the whole page reflects one moment rather than eight separate
 * queries settling at different times.
 */
public class StatisticsData {

    public int reviewsToday;
    public int reviewsThisWeek;
    public int totalReviews;

    public int streakDays;

    /** Percent of answers better than Again over the window; -1 when no data. */
    public int retentionPercent;

    public long timeTodayMillis;

    public CardStateBreakdown breakdown;

    /** Reviews per day for the chart, oldest first. */
    public int[] dailyCounts = new int[0];

    /** Short labels under the chart bars, aligned with {@link #dailyCounts}. */
    public String[] dailyLabels = new String[0];

    public boolean hasAnyActivity() {
        return totalReviews > 0;
    }
}

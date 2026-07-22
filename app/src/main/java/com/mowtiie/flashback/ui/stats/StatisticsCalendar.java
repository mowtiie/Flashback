package com.mowtiie.flashback.ui.stats;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Day-boundary helpers for the statistics screen. Stats use calendar days
 * (local midnight), not the study rollover: a person reading a progress page
 * thinks in ordinary days, and the scheduler's 4am rollover is an internal
 * detail. Kept apart so that choice is in one place.
 */
final class StatisticsCalendar {

    private StatisticsCalendar() {
    }

    static long startOfToday(long now, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    static long startOfDaysAgo(long now, int days, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(startOfToday(now, zone));
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return cal.getTimeInMillis();
    }

    static String todayKey(long now, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(now);
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    /** yyyy-MM-dd key for {@code offset} days before today (offset >= 0). */
    static String dayKey(long now, int offset, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_YEAR, -offset);
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    /** Day-of-month label for {@code offset} days before today. */
    static String dayLabel(long now, int offset, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_YEAR, -offset);
        return String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
    }
}

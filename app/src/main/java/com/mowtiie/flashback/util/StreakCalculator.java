package com.mowtiie.flashback.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Counts consecutive study days ending at today.
 *
 * <p>Kept free of Android and of {@code java.time} (which needs API 26 or
 * desugaring on this project's min-24 floor) so it can be unit tested on the
 * JVM. Dates are the {@code yyyy-MM-dd} strings SQLite's {@code date()} already
 * produces; they are converted to integer epoch-days with the standard
 * days-from-civil algorithm, which is exact and needs no calendar object.
 */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * @param studyDaysDesc distinct {@code yyyy-MM-dd} study days, any order
     * @param today         today's date as {@code yyyy-MM-dd}
     * @return the length of the current streak; 0 if neither today nor
     *         yesterday saw any study
     */
    public static int compute(List<String> studyDaysDesc, String today) {
        if (studyDaysDesc == null || studyDaysDesc.isEmpty() || today == null) {
            return 0;
        }

        Set<Long> days = new HashSet<>();
        for (String day : studyDaysDesc) {
            Long epoch = toEpochDay(day);
            if (epoch != null) {
                days.add(epoch);
            }
        }

        Long todayEpoch = toEpochDay(today);
        if (todayEpoch == null) {
            return 0;
        }

        // A streak is still "alive" today until the user studies: if today has
        // no reviews yet but yesterday does, count from yesterday. If neither
        // has any, the streak is broken.
        long cursor;
        if (days.contains(todayEpoch)) {
            cursor = todayEpoch;
        } else if (days.contains(todayEpoch - 1)) {
            cursor = todayEpoch - 1;
        } else {
            return 0;
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor--;
        }
        return streak;
    }

    /** Parses {@code yyyy-MM-dd}, returning null on anything malformed. */
    static Long toEpochDay(String date) {
        if (date == null || date.length() < 10) {
            return null;
        }
        try {
            int year = Integer.parseInt(date.substring(0, 4));
            int month = Integer.parseInt(date.substring(5, 7));
            int day = Integer.parseInt(date.substring(8, 10));
            return epochDay(year, month, day);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Howard Hinnant's days-from-civil: exact integer date arithmetic. */
    static long epochDay(int year, int month, int day) {
        int y = year - (month <= 2 ? 1 : 0);
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153L * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
}

package com.mowtiie.flashback.notifications;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Works out how long until the next occurrence of a wall-clock time. Pulled out
 * of the scheduler and kept free of Android types so the day-boundary logic —
 * which is the part that is easy to get wrong — can be unit tested on the JVM.
 */
public final class ReminderTime {

    private ReminderTime() {
    }

    /**
     * Milliseconds from {@code now} until the next {@code hour}:{@code minute}.
     * If that time has already passed today, returns the delay to tomorrow's.
     * Exactly-now counts as passed, so a worker firing at its target reschedules
     * a full day out rather than immediately re-firing.
     */
    public static long millisUntilNext(long now, int hour, int minute, TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis() - now;
    }
}

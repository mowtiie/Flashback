package com.mowtiie.flashback.notifications;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ReminderTimeTest {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final long HOUR = 3_600_000L;
    private static final long DAY = 86_400_000L;

    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance(UTC);
        cal.set(year, month, day, hour, minute, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Test
    public void targetLaterTodayReturnsShortDelay() {
        assertEquals(HOUR, ReminderTime.millisUntilNext(at(2024, 0, 15, 8, 0), 9, 0, UTC));
    }

    @Test
    public void targetAlreadyPassedRollsToTomorrow() {
        assertEquals(23 * HOUR,
                ReminderTime.millisUntilNext(at(2024, 0, 15, 10, 0), 9, 0, UTC));
    }

    @Test
    public void exactlyNowSchedulesAFullDayOutNotZero() {
        assertEquals(DAY, ReminderTime.millisUntilNext(at(2024, 0, 15, 9, 0), 9, 0, UTC));
    }

    @Test
    public void minutesAreHonoured() {
        assertEquals(30 * 60_000L,
                ReminderTime.millisUntilNext(at(2024, 0, 15, 9, 0), 9, 30, UTC));
    }

    @Test
    public void midnightTargetFromLateEvening() {
        assertEquals(HOUR, ReminderTime.millisUntilNext(at(2024, 0, 15, 23, 0), 0, 0, UTC));
    }

    @Test
    public void justPastMidnightTargetRollsToTomorrow() {
        assertEquals(DAY - 60_000L,
                ReminderTime.millisUntilNext(at(2024, 0, 15, 0, 1), 0, 0, UTC));
    }
}

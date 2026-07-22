package com.mowtiie.flashback.notifications;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Enqueues and cancels the daily reminder.
 *
 * <p>A self-rescheduling one-time worker, not periodic work. Periodic work only
 * guarantees one run per interval, so its fire time drifts within the period; a
 * one-time job re-anchored to the exact clock time each day keeps "20:00" at
 * 20:00. WorkManager persists pending work across reboots in its own database,
 * so no boot receiver is needed to survive a restart.
 *
 * <p>{@link #schedule} is idempotent: it always computes the next occurrence
 * from now, so calling it on app start, on a settings change, and from the
 * worker itself all do the right thing.
 */
public final class ReminderScheduler {

    static final String WORK_NAME = "flashback_daily_reminder";

    private ReminderScheduler() {
    }

    public static void schedule(Context context) {
        ReminderPreferences prefs = new ReminderPreferences(context);
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());

        if (!prefs.isEnabled()) {
            workManager.cancelUniqueWork(WORK_NAME);
            return;
        }

        long delay = ReminderTime.millisUntilNext(
                System.currentTimeMillis(),
                prefs.getHour(),
                prefs.getMinute(),
                TimeZone.getDefault());

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build();

        // REPLACE, not KEEP: a settings change or a timezone shift should move
        // the pending job, not be ignored because one already exists.
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME);
    }
}

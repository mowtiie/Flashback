package com.mowtiie.flashback.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mowtiie.flashback.repository.FlashbackRepository;

/**
 * Runs at the chosen time, posts the reminder if anything is due, then
 * reschedules itself for the next day. {@link Worker} already runs off the main
 * thread, so the blocking count query is called directly.
 */
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        int due = FlashbackRepository.getInstance(context)
                .countDueEverywhereBlocking(System.currentTimeMillis());
        if (due > 0) {
            ReminderNotifier.notifyDue(context, due);
        }

        // Re-anchor for tomorrow. Because the target time is in the past now,
        // millisUntilNext lands on the next day.
        ReminderScheduler.schedule(context);

        return Result.success();
    }
}

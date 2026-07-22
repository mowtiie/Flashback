package com.mowtiie.flashback.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mowtiie.flashback.MainActivity;
import com.mowtiie.flashback.R;

/**
 * Owns the notification channel and the single summary notification. One
 * notification, reused by id, rather than one per card — a wall of
 * notifications is the fastest way to get an app uninstalled.
 */
public final class ReminderNotifier {

    private static final String CHANNEL_ID = "study_reminders";
    private static final int NOTIFICATION_ID = 1;

    private ReminderNotifier() {
    }

    /** Safe to call repeatedly; creating an existing channel is a no-op. */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_body));

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void notifyDue(Context context, int dueCount) {
        ensureChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = context.getResources().getQuantityString(
                R.plurals.reminder_body, dueCount, dueCount);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pending);

        // From API 33 a posted notification is silently dropped without the
        // runtime permission; checking keeps us from assuming it succeeded.
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || androidx.core.content.ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}

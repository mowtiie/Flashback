package com.mowtiie.flashback;

import android.app.Application;

import com.google.android.material.color.DynamicColors;
import com.mowtiie.flashback.notifications.ReminderNotifier;
import com.mowtiie.flashback.notifications.ReminderScheduler;

public class FlashbackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Android 12+ recolours every activity from the user's wallpaper.
        // Older devices fall back to the palette in res/values/colors.xml.
        DynamicColors.applyToActivitiesIfAvailable(this);

        // Channel must exist before any notification is posted; cheap to ensure
        // on every start. Rescheduling re-anchors the daily reminder in case a
        // reboot or timezone change moved it.
        ReminderNotifier.ensureChannel(this);
        ReminderScheduler.schedule(this);
    }
}

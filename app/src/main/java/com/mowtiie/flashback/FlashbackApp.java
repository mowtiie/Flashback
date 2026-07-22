package com.mowtiie.flashback;

import android.app.Application;

import com.mowtiie.flashback.notifications.ReminderNotifier;
import com.mowtiie.flashback.notifications.ReminderScheduler;
import com.mowtiie.flashback.theme.ThemeController;

public class FlashbackApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Night mode is global; dynamic colour and contrast are applied per
        // activity in BaseActivity so the toggle and overlay can coordinate.
        ThemeController.init(this);

        // Channel must exist before any notification is posted; cheap to ensure
        // on every start. Rescheduling re-anchors the daily reminder in case a
        // reboot or timezone change moved it.
        ReminderNotifier.ensureChannel(this);
        ReminderScheduler.schedule(this);
    }
}

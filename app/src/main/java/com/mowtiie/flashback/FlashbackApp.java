package com.mowtiie.flashback;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class FlashbackApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}

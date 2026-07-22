package com.mowtiie.flashback;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class FlashbackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Android 12+ recolours every activity from the user's wallpaper.
        // Older devices fall back to the palette in res/values/colors.xml.
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}

package com.mowtiie.flashback.theme;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The three appearance settings, in the same prefs file everything else uses.
 *
 * <p>Theme mode maps to AppCompat's night-mode constants. Contrast selects one
 * of the Material Theme Builder overlay sets and only takes effect when dynamic
 * colour is off — dynamic colour brings its own tonal palette and, on Android
 * 14+, follows the system contrast slider directly.
 */
public class ThemePreferences {

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    public static final int CONTRAST_STANDARD = 0;
    public static final int CONTRAST_MEDIUM = 1;
    public static final int CONTRAST_HIGH = 2;

    private static final String FILE = "flashback_prefs";
    private static final String KEY_MODE = "theme_mode";
    private static final String KEY_CONTRAST = "theme_contrast";
    private static final String KEY_DYNAMIC = "theme_dynamic_color";

    private final SharedPreferences prefs;

    public ThemePreferences(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_MODE, MODE_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_MODE, mode).apply();
    }

    public int getContrast() {
        return prefs.getInt(KEY_CONTRAST, CONTRAST_STANDARD);
    }

    public void setContrast(int contrast) {
        prefs.edit().putInt(KEY_CONTRAST, contrast).apply();
    }

    /** Defaults on: dynamic colour is the expected Material 3 behaviour on 12+. */
    public boolean isDynamicColor() {
        return prefs.getBoolean(KEY_DYNAMIC, true);
    }

    public void setDynamicColor(boolean enabled) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply();
    }
}

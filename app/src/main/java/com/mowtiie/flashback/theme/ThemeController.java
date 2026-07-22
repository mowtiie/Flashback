package com.mowtiie.flashback.theme;

import android.app.Activity;
import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.mowtiie.flashback.R;

/**
 * Applies the appearance settings. Two moments matter:
 *
 * <ul>
 *   <li>Night mode is global and set once, from {@code Application}, via
 *       AppCompat — it recreates activities as needed.</li>
 *   <li>Dynamic colour and the contrast overlay are per-activity and must be
 *       applied in {@code onCreate} before {@code setContentView}, or the
 *       inflated views keep the old colours. {@link #applyToActivity} does that
 *       and is called from a base activity.</li>
 * </ul>
 */
public final class ThemeController {

    private ThemeController() {
    }

    /** Global night mode. Safe to call from Application.onCreate. */
    public static void applyThemeMode(ThemePreferences prefs) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(prefs.getThemeMode()));
    }

    private static int toNightMode(int mode) {
        switch (mode) {
            case ThemePreferences.MODE_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case ThemePreferences.MODE_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /**
     * Applies dynamic colour or the contrast overlay to one activity. Must run
     * before setContentView.
     *
     * <p>Dynamic colour wins when enabled and available: it supplies its own
     * palette, so a hand-authored contrast overlay would fight it. Only when
     * dynamic colour is off does the contrast overlay apply.
     */
    public static void applyToActivity(Activity activity, ThemePreferences prefs) {
        boolean dynamicApplied = false;
        if (prefs.isDynamicColor() && DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(activity,
                    new DynamicColorsOptions.Builder().build());
            dynamicApplied = true;
        }

        if (!dynamicApplied) {
            int overlay = contrastOverlay(prefs.getContrast());
            if (overlay != 0) {
                activity.getTheme().applyStyle(overlay, true);
            }
        }
    }

    private static int contrastOverlay(int contrast) {
        switch (contrast) {
            case ThemePreferences.CONTRAST_MEDIUM:
                return R.style.ThemeOverlay_Flashback_MediumContrast;
            case ThemePreferences.CONTRAST_HIGH:
                return R.style.ThemeOverlay_Flashback_HighContrast;
            default:
                return 0;
        }
    }

    /**
     * Registers app-wide dynamic colour is intentionally NOT done here anymore;
     * it is per-activity through {@link #applyToActivity} so the toggle and the
     * contrast overlay can coordinate. Kept as a no-op hook for Application.
     */
    public static void init(Application application) {
        // Night mode is the only global piece.
        applyThemeMode(new ThemePreferences(application));
    }
}

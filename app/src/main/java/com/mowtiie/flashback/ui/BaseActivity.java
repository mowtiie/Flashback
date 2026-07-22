package com.mowtiie.flashback.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mowtiie.flashback.theme.ThemeController;
import com.mowtiie.flashback.theme.ThemePreferences;

/**
 * Applies dynamic colour and the contrast overlay before the content view is
 * inflated. Every activity extends this so the appearance settings take effect
 * consistently and in the one place where the ordering is correct.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeController.applyToActivity(this, new ThemePreferences(this));
        super.onCreate(savedInstanceState);
    }
}

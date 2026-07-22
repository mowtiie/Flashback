package com.mowtiie.flashback.util;

import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.mowtiie.flashback.R;

/**
 * Wires a fragment's toolbar to the nav controller. Declaring the three tabs as
 * top level is what decides whether a screen shows a back arrow, so it is done
 * in one place rather than per fragment.
 */
public final class Toolbars {

    private static final AppBarConfiguration CONFIG = new AppBarConfiguration.Builder(
            R.id.deckListFragment,
            R.id.tagsFragment,
            R.id.settingsFragment).build();

    private Toolbars() {
    }

    public static void setup(MaterialToolbar toolbar, NavController navController) {
        NavigationUI.setupWithNavController(toolbar, navController, CONFIG);
    }
}

package com.mowtiie.flashback;

import android.os.Bundle;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.mowtiie.flashback.databinding.ActivityMainBinding;
import com.mowtiie.flashback.ui.BaseActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Hosts the one shared app bar and the bottom navigation. Fragment screens no
 * longer carry their own toolbar; they contribute titles through the nav graph
 * labels and actions through the MenuProvider API. The study screen is the sole
 * exception — it keeps its own bar because of the counts strip beneath it — and
 * hides this shared one while it is showing.
 */
public class MainActivity extends BaseActivity {

    /** Tabs, not pushed screens: these show no back arrow. */
    private static final Set<Integer> TOP_LEVEL = new HashSet<>(Arrays.asList(
            R.id.deckListFragment,
            R.id.tagsFragment,
            R.id.settingsFragment));

    /** Destinations that manage their own app bar, so the shared one hides. */
    private static final Set<Integer> OWN_APP_BAR = new HashSet<>(
            Arrays.asList(R.id.studyFragment));

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment host = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.navHost);
        if (host == null) {
            throw new IllegalStateException("NavHostFragment missing from activity_main");
        }
        NavController navController = host.getNavController();

        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(TOP_LEVEL).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        navController.addOnDestinationChangedListener(this::onDestinationChanged);
    }

    private void onDestinationChanged(@androidx.annotation.NonNull NavController controller,
                                      @androidx.annotation.NonNull NavDestination destination,
                                      Bundle arguments) {
        int id = destination.getId();

        binding.bottomNav.setVisibility(
                TOP_LEVEL.contains(id) ? View.VISIBLE : View.GONE);

        // A screen with its own app bar suppresses the shared one entirely.
        boolean ownsBar = OWN_APP_BAR.contains(id);
        binding.appBar.setVisibility(ownsBar ? View.GONE : View.VISIBLE);
    }

    /** The shared app bar, so scrolling fragments can drive its lifted state. */
    public com.google.android.material.appbar.AppBarLayout getAppBar() {
        return binding == null ? null : binding.appBar;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment host = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.navHost);
        return (host != null && host.getNavController().navigateUp())
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

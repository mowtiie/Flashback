package com.mowtiie.flashback;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.mowtiie.flashback.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final Set<Integer> TOP_LEVEL = new HashSet<>(Arrays.asList(
            R.id.deckListFragment,
            R.id.tagsFragment,
            R.id.settingsFragment));

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.navHost);
        if (host == null) {
            throw new IllegalStateException("NavHostFragment missing from activity_main");
        }
        NavController navController = host.getNavController();

        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> {
                    boolean topLevel = TOP_LEVEL.contains(destination.getId());
                    binding.bottomNav.setVisibility(topLevel ? View.VISIBLE : View.GONE);
                });
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

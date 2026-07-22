package com.mowtiie.flashback.ui.stats;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.data.model.CardStateBreakdown;
import com.mowtiie.flashback.databinding.FragmentStatisticsBinding;
import com.mowtiie.flashback.util.Toolbars;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private StatisticsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        NavController navController = NavHostFragment.findNavController(this);
        Toolbars.setup(binding.toolbar, navController);

        binding.tileReviews.tileLabel.setText(R.string.stats_reviews_today);
        binding.tileStreak.tileLabel.setText(R.string.stats_streak);
        binding.tileRetention.tileLabel.setText(R.string.stats_retention);
        binding.tileTime.tileLabel.setText(R.string.stats_time_today);

        binding.emptyState.emptyIcon.setImageResource(R.drawable.ic_stats);
        binding.emptyState.emptyTitle.setText(R.string.stats_empty_title);
        binding.emptyState.emptyBody.setText(R.string.stats_empty_body);
        binding.emptyState.emptyAction.setVisibility(View.GONE);

        viewModel.getData().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(StatisticsData data) {
        if (data == null) {
            return;
        }

        boolean hasActivity = data.hasAnyActivity() || data.breakdown.total() > 0;
        binding.emptyState.getRoot().setVisibility(hasActivity ? View.GONE : View.VISIBLE);
        binding.statsScroll.setVisibility(hasActivity ? View.VISIBLE : View.GONE);
        if (!hasActivity) {
            return;
        }

        binding.tileReviews.tileValue.setText(String.valueOf(data.reviewsToday));
        binding.tileStreak.tileValue.setText(getResources().getQuantityString(
                R.plurals.stats_streak_value, data.streakDays, data.streakDays));
        binding.tileRetention.tileValue.setText(data.retentionPercent < 0
                ? getString(R.string.stats_no_value)
                : getString(R.string.stats_percent, data.retentionPercent));
        binding.tileTime.tileValue.setText(formatDuration(data.timeTodayMillis));

        binding.activityChart.setData(data.dailyCounts, data.dailyLabels);

        renderBreakdown(data.breakdown);
    }

    private void renderBreakdown(CardStateBreakdown breakdown) {
        int newColor = ContextCompat.getColor(requireContext(), R.color.count_new);
        int learnColor = ContextCompat.getColor(requireContext(), R.color.count_learning);
        int youngColor = ContextCompat.getColor(requireContext(), R.color.tag_teal);
        int matureColor = ContextCompat.getColor(requireContext(), R.color.count_due);

        binding.breakdownBar.setSegments(
                new int[]{breakdown.newCount, breakdown.learningCount,
                        breakdown.youngCount, breakdown.matureCount},
                new int[]{newColor, learnColor, youngColor, matureColor});

        binding.breakdownTotal.setText(getResources().getQuantityString(
                R.plurals.stats_total_cards, breakdown.total(), breakdown.total()));

        binding.breakdownLegend.removeAllViews();
        addLegend(getString(R.string.count_new), breakdown.newCount, newColor);
        addLegend(getString(R.string.count_learning), breakdown.learningCount, learnColor);
        addLegend(getString(R.string.stats_young), breakdown.youngCount, youngColor);
        addLegend(getString(R.string.stats_mature), breakdown.matureCount, matureColor);
    }

    private void addLegend(String label, int count, int color) {
        Chip chip = new Chip(requireContext());
        chip.setText(getString(R.string.stats_legend_entry, label, count));
        chip.setClickable(false);
        chip.setChipIconVisible(true);
        chip.setChipIconTint(android.content.res.ColorStateList.valueOf(color));
        chip.setChipIconResource(R.drawable.ic_dot);
        chip.setEnsureMinTouchTargetSize(false);
        binding.breakdownLegend.addView(chip);
    }

    /** Whole minutes below an hour, one decimal of an hour above. */
    private String formatDuration(long millis) {
        long minutes = millis / DateUtils.MINUTE_IN_MILLIS;
        if (minutes < 60) {
            return getString(R.string.stats_minutes, minutes);
        }
        return getString(R.string.stats_hours,
                String.format(java.util.Locale.getDefault(), "%.1f", minutes / 60f));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

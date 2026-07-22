package com.mowtiie.flashback.ui.decks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mowtiie.flashback.MainActivity;
import com.mowtiie.flashback.ui.AppBarLift;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.databinding.FragmentDeckListBinding;

import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.List;

public class DeckListFragment extends Fragment {

    private FragmentDeckListBinding binding;
    private DeckListViewModel viewModel;
    private DeckAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDeckListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DeckListViewModel.class);
        NavController navController = NavHostFragment.findNavController(this);

        adapter = new DeckAdapter(item -> {
            Bundle args = new Bundle();
            args.putLong("deckId", item.summary.deck.id);
            navController.navigate(R.id.action_deckList_to_deckDetail, args);
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_deck_list, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.action_statistics) {
                    navController.navigate(R.id.action_deckList_to_statistics);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        binding.deckList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.deckList.setAdapter(adapter);

        if (requireActivity() instanceof MainActivity) {
            AppBarLift.attach(((MainActivity) requireActivity()).getAppBar(), binding.deckList);
        }

        binding.addDeck.setOnClickListener(v ->
                navController.navigate(R.id.action_deckList_to_deckEditor));

        binding.emptyState.emptyTitle.setText(R.string.decks_empty_title);
        binding.emptyState.emptyBody.setText(R.string.decks_empty_body);
        binding.emptyState.emptyAction.setText(R.string.decks_empty_action);
        binding.emptyState.emptyAction.setOnClickListener(v ->
                navController.navigate(R.id.action_deckList_to_deckEditor));

        viewModel.getAllTags().observe(getViewLifecycleOwner(), this::buildFilterChips);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            boolean empty = items == null || items.isEmpty();
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.deckList.setVisibility(empty ? View.GONE : View.VISIBLE);
            // Hiding the FAB while the empty state is up avoids two competing
            // calls to the same action.
            binding.addDeck.setVisibility(empty ? View.GONE : View.VISIBLE);

            // A filter that hides everything is not the same as having no
            // decks, so say which one the user is looking at.
            boolean filtered = viewModel.getSelectedTagId() != null;
            binding.emptyState.emptyTitle.setText(filtered
                    ? R.string.decks_filtered_empty_title : R.string.decks_empty_title);
            binding.emptyState.emptyBody.setText(filtered
                    ? R.string.decks_filtered_empty_body : R.string.decks_empty_body);
            binding.emptyState.emptyAction.setVisibility(filtered ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Rebuilds the tag filter row. The whole row stays hidden until a tag
     * exists, so the feature is invisible to anyone not using it.
     */
    private void buildFilterChips(List<Tag> tags) {
        binding.filterChips.removeAllViews();
        boolean hasTags = tags != null && !tags.isEmpty();
        binding.filterScroll.setVisibility(hasTags ? View.VISIBLE : View.GONE);
        if (!hasTags) {
            return;
        }

        // A tag can be deleted while it is the active filter, which would
        // otherwise leave the list permanently empty with no way back.
        Long active = viewModel.getSelectedTagId();
        if (active != null && !containsTag(tags, active)) {
            viewModel.selectTag(null);
        }

        Chip all = newFilterChip(getString(R.string.filter_all));
        all.setChecked(viewModel.getSelectedTagId() == null);
        all.setOnClickListener(v -> viewModel.selectTag(null));
        binding.filterChips.addView(all);

        Long selected = viewModel.getSelectedTagId();
        for (Tag tag : tags) {
            Chip chip = newFilterChip(tag.name);
            chip.setChecked(selected != null && selected == tag.id);
            chip.setOnClickListener(v -> viewModel.selectTag(tag.id));
            binding.filterChips.addView(chip);
        }
    }

    private boolean containsTag(List<Tag> tags, long tagId) {
        for (Tag tag : tags) {
            if (tag.id == tagId) {
                return true;
            }
        }
        return false;
    }

    private Chip newFilterChip(String label) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        return chip;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recompute due counts against the current time.
        viewModel.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.deckList.setAdapter(null);
        binding = null;
    }
}

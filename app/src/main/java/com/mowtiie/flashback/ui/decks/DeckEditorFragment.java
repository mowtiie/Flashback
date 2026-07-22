package com.mowtiie.flashback.ui.decks;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.databinding.FragmentDeckEditorBinding;
import com.mowtiie.flashback.util.Toolbars;
import com.mowtiie.flashback.util.ViewModelFactory;

import java.util.List;
import java.util.Set;

public class DeckEditorFragment extends Fragment {

    /** Argument value meaning "create a deck" rather than edit one. */
    public static final long NEW_ID = -1L;

    private static final int DEFAULT_NEW_PER_DAY = 20;
    private static final int DEFAULT_REVIEWS_PER_DAY = 200;

    private FragmentDeckEditorBinding binding;
    private DeckEditorViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDeckEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long deckId = getArguments() == null
                ? NEW_ID : getArguments().getLong("deckId", NEW_ID);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(
                () -> new DeckEditorViewModel(requireActivity().getApplication(), deckId)))
                .get(DeckEditorViewModel.class);

        NavController navController = NavHostFragment.findNavController(this);

        Toolbars.setup(binding.toolbar, navController);
        binding.toolbar.setTitle(viewModel.isEditing()
                ? R.string.deck_editor_edit_title : R.string.deck_editor_new_title);

        if (!viewModel.isEditing()) {
            binding.newPerDay.setText(String.valueOf(DEFAULT_NEW_PER_DAY));
            binding.reviewsPerDay.setText(String.valueOf(DEFAULT_REVIEWS_PER_DAY));
        }

        viewModel.getDeck().observe(getViewLifecycleOwner(), deck -> {
            if (deck == null) {
                return;
            }
            binding.deckName.setText(deck.name);
            binding.deckDescription.setText(deck.description);
            binding.reverseDefault.setChecked(deck.reverseByDefault);
            binding.newPerDay.setText(String.valueOf(deck.newPerDay));
            binding.reviewsPerDay.setText(String.valueOf(deck.reviewsPerDay));
        });

        viewModel.getSaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) {
                navController.navigateUp();
            }
        });

        binding.newTag.setOnClickListener(v ->
                navController.navigate(R.id.action_deckEditor_to_tagEditor));

        viewModel.getAllTags().observe(getViewLifecycleOwner(), this::buildTagChips);
        viewModel.getSelectedTagIds().observe(getViewLifecycleOwner(), ids -> applyChecks(ids));

        binding.saveDeck.setOnClickListener(v -> onSave());
    }

    /**
     * One filter chip per tag. Rebuilt whenever the tag list changes, which
     * covers returning from the tag editor with a newly created tag.
     */
    private void buildTagChips(List<Tag> tags) {
        binding.deckTagChips.removeAllViews();
        boolean hasTags = tags != null && !tags.isEmpty();
        binding.noTagsHint.setVisibility(hasTags ? View.GONE : View.VISIBLE);
        if (!hasTags) {
            return;
        }

        Set<Long> selected = viewModel.getSelectedTagIds().getValue();
        for (Tag tag : tags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag.name);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setTag(tag.id);
            chip.setChecked(selected != null && selected.contains(tag.id));
            chip.setOnCheckedChangeListener((button, checked) ->
                    viewModel.toggleTag(tag.id, checked));
            binding.deckTagChips.addView(chip);
        }
    }

    /** Syncs chip state when the selection is seeded from the saved deck. */
    private void applyChecks(Set<Long> ids) {
        if (ids == null) {
            return;
        }
        for (int i = 0; i < binding.deckTagChips.getChildCount(); i++) {
            View child = binding.deckTagChips.getChildAt(i);
            if (child instanceof Chip && child.getTag() instanceof Long) {
                Chip chip = (Chip) child;
                boolean shouldCheck = ids.contains((Long) child.getTag());
                if (chip.isChecked() != shouldCheck) {
                    chip.setChecked(shouldCheck);
                }
            }
        }
    }

    private void onSave() {
        String name = text(binding.deckName);
        binding.deckNameLayout.setError(null);
        if (TextUtils.isEmpty(name)) {
            binding.deckNameLayout.setError(getString(R.string.deck_name_required));
            binding.deckName.requestFocus();
            return;
        }

        viewModel.save(
                name,
                text(binding.deckDescription),
                binding.reverseDefault.isChecked(),
                parsePositiveInt(text(binding.newPerDay), DEFAULT_NEW_PER_DAY),
                parsePositiveInt(text(binding.reviewsPerDay), DEFAULT_REVIEWS_PER_DAY));
    }

    private String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    /** Empty or nonsense input falls back to the default rather than crashing. */
    private int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

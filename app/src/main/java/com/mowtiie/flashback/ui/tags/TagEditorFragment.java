package com.mowtiie.flashback.ui.tags;

import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.databinding.FragmentTagEditorBinding;

import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mowtiie.flashback.util.TagChips;
import com.mowtiie.flashback.util.ViewModelFactory;

public class TagEditorFragment extends Fragment {

    public static final long NEW_ID = -1L;

    private static final int SWATCH_COLUMNS = 6;

    private FragmentTagEditorBinding binding;
    private TagEditorViewModel viewModel;
    private ColorSwatchAdapter swatchAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTagEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long tagId = getArguments() == null ? NEW_ID : getArguments().getLong("tagId", NEW_ID);
        int[] palette = TagChips.palette(requireContext());

        viewModel = new ViewModelProvider(this, new ViewModelFactory(
                () -> new TagEditorViewModel(
                        requireActivity().getApplication(), tagId, palette[0])))
                .get(TagEditorViewModel.class);

        NavController navController = NavHostFragment.findNavController(this);

        boolean editing = viewModel.isEditing();
        requireActivity().setTitle(editing ? R.string.tag_edit_title : R.string.tag_new_title);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_tag_editor, menu);
                menu.findItem(R.id.action_delete_tag).setVisible(editing);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.action_delete_tag) {
                    confirmDelete(navController);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        swatchAdapter = new ColorSwatchAdapter(palette, palette[0],
                colour -> viewModel.selectColour(colour));
        binding.colourGrid.setLayoutManager(
                new GridLayoutManager(requireContext(), SWATCH_COLUMNS));
        binding.colourGrid.setAdapter(swatchAdapter);

        viewModel.getTag().observe(getViewLifecycleOwner(), tag -> {
            if (tag == null) {
                return;
            }
            binding.tagName.setText(tag.name);
            binding.tagDescription.setText(tag.description);
        });

        // The preview is the point of the colour grid: it shows the chip
        // exactly as it will appear on a deck card, in the current theme.
        viewModel.getColour().observe(getViewLifecycleOwner(), colour -> {
            if (colour == null) {
                return;
            }
            swatchAdapter.setSelected(colour);
            updatePreview(colour);
        });

        binding.tagName.addTextChangedListener(new SimpleWatcher(() -> {
            Integer colour = viewModel.getColour().getValue();
            updatePreview(colour == null ? 0 : colour);
        }));

        viewModel.getFinished().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                navController.navigateUp();
            }
        });

        binding.saveTag.setOnClickListener(v -> onSave());

        if (requireActivity() instanceof MainActivity) {
            AppBarLift.attach(((MainActivity) requireActivity()).getAppBar(), binding.tagEditorScroll);
        }
    }

    private void updatePreview(int colour) {
        Tag preview = new Tag();
        String typed = text(binding.tagName);
        preview.name = TextUtils.isEmpty(typed) ? getString(R.string.tag_preview_placeholder) : typed;
        preview.color = colour;
        TagChips.apply(binding.tagPreview, preview);
    }

    private void onSave() {
        String name = text(binding.tagName);
        binding.tagNameLayout.setError(null);
        if (TextUtils.isEmpty(name)) {
            binding.tagNameLayout.setError(getString(R.string.tag_name_required));
            binding.tagName.requestFocus();
            return;
        }
        viewModel.save(name, text(binding.tagDescription));
    }

    private void confirmDelete(NavController navController) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.tag_delete_title)
                .setMessage(R.string.tag_delete_body)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.delete();
                    navController.navigateUp();
                })
                .show();
    }

    private String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.colourGrid.setAdapter(null);
        binding = null;
    }

    /** Trims TextWatcher down to the one callback that is ever needed. */
    private static class SimpleWatcher implements android.text.TextWatcher {

        private final Runnable onChanged;

        SimpleWatcher(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
            onChanged.run();
        }
    }
}

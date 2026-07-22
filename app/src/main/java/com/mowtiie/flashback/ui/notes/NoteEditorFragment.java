package com.mowtiie.flashback.ui.notes;

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.databinding.FragmentNoteEditorBinding;

import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mowtiie.flashback.util.ViewModelFactory;

public class NoteEditorFragment extends Fragment {

    /** Argument value meaning "create a card" rather than edit one. */
    public static final long NEW_ID = -1L;

    private FragmentNoteEditorBinding binding;
    private NoteEditorViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNoteEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long deckId = requireArguments().getLong("deckId");
        long noteId = requireArguments().getLong("noteId", NEW_ID);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(
                () -> new NoteEditorViewModel(
                        requireActivity().getApplication(), deckId, noteId)))
                .get(NoteEditorViewModel.class);

        NavController navController = NavHostFragment.findNavController(this);

        boolean editing = viewModel.isEditing();
        requireActivity().setTitle(editing
                ? R.string.note_editor_edit_title : R.string.note_editor_new_title);

        // Bulk entry only makes sense when creating.
        binding.saveAndAddNote.setVisibility(editing ? View.GONE : View.VISIBLE);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_note_editor, menu);
                // Delete only applies to an existing card.
                menu.findItem(R.id.action_delete_note).setVisible(editing);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.action_delete_note) {
                    confirmDelete(navController);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        viewModel.getNote().observe(getViewLifecycleOwner(), note -> {
            if (note == null) {
                binding.noteReverse.setChecked(viewModel.getReverseDefault());
                return;
            }
            binding.noteFront.setText(note.front);
            binding.noteBack.setText(note.back);
            binding.noteReverse.setChecked(note.reverseEnabled);
        });

        viewModel.getSaveOutcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome == null) {
                return;
            }
            if (outcome == NoteEditorViewModel.SaveOutcome.CLOSE) {
                navController.navigateUp();
            } else {
                clearForNextCard();
            }
        });

        binding.saveNote.setOnClickListener(v -> attemptSave(false));

        if (requireActivity() instanceof MainActivity) {
            AppBarLift.attach(((MainActivity) requireActivity()).getAppBar(), binding.noteEditorScroll);
        }
        binding.saveAndAddNote.setOnClickListener(v -> attemptSave(true));
    }

    private void attemptSave(boolean addAnother) {
        String front = text(binding.noteFront);
        String back = text(binding.noteBack);

        binding.noteFrontLayout.setError(null);
        binding.noteBackLayout.setError(null);

        if (TextUtils.isEmpty(front)) {
            binding.noteFrontLayout.setError(getString(R.string.note_front_required));
            binding.noteFront.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(back)) {
            binding.noteBackLayout.setError(getString(R.string.note_back_required));
            binding.noteBack.requestFocus();
            return;
        }

        viewModel.save(front, back, binding.noteReverse.isChecked(), addAnother);
    }

    /**
     * Keeps the reverse switch as the user left it, since a run of cards
     * entered together almost always wants the same direction setting.
     */
    private void clearForNextCard() {
        binding.noteFront.setText("");
        binding.noteBack.setText("");
        binding.noteFront.requestFocus();
        Snackbar.make(binding.getRoot(), R.string.note_saved, Snackbar.LENGTH_SHORT).show();
    }

    private void confirmDelete(NavController navController) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.note_delete_title)
                .setMessage(R.string.note_delete_body)
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
        binding = null;
    }
}

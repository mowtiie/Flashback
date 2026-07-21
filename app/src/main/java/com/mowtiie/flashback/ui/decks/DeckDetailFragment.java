package com.mowtiie.flashback.ui.decks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.databinding.FragmentDeckDetailBinding;
import com.mowtiie.flashback.util.Toolbars;
import com.mowtiie.flashback.util.ViewModelFactory;

public class DeckDetailFragment extends Fragment {

    private FragmentDeckDetailBinding binding;
    private DeckDetailViewModel viewModel;
    private NoteAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDeckDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long deckId = requireArguments().getLong("deckId");
        viewModel = new ViewModelProvider(this, new ViewModelFactory(
                () -> new DeckDetailViewModel(requireActivity().getApplication(), deckId)))
                .get(DeckDetailViewModel.class);

        NavController navController = NavHostFragment.findNavController(this);
        Toolbars.setup(binding.toolbar, navController);

        adapter = new NoteAdapter(note -> {
            Bundle args = new Bundle();
            args.putLong("deckId", deckId);
            args.putLong("noteId", note.id);
            navController.navigate(R.id.action_deckDetail_to_noteEditor, args);
        });

        binding.noteList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.noteList.setAdapter(adapter);

        binding.addNote.setOnClickListener(v -> openNewNote(navController, deckId));

        binding.emptyState.emptyTitle.setText(R.string.deck_detail_empty_title);
        binding.emptyState.emptyBody.setText(R.string.deck_detail_empty_body);
        binding.emptyState.emptyAction.setText(R.string.deck_detail_empty_action);
        binding.emptyState.emptyAction.setOnClickListener(
                v -> openNewNote(navController, deckId));

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit_deck) {
                Bundle args = new Bundle();
                args.putLong("deckId", deckId);
                navController.navigate(R.id.action_deckDetail_to_deckEditor, args);
                return true;
            }
            if (id == R.id.action_delete_deck) {
                confirmDeleteDeck(navController);
                return true;
            }
            return false;
        });

        viewModel.getDeck().observe(getViewLifecycleOwner(), deck -> {
            if (deck != null) {
                binding.toolbar.setTitle(deck.name);
            }
        });

        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            boolean empty = notes == null || notes.isEmpty();
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.noteList.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.addNote.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void openNewNote(NavController navController, long deckId) {
        Bundle args = new Bundle();
        args.putLong("deckId", deckId);
        navController.navigate(R.id.action_deckDetail_to_noteEditor, args);
    }

    private void confirmDeleteDeck(NavController navController) {
        String name = viewModel.getDeck().getValue() == null
                ? "" : viewModel.getDeck().getValue().name;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.deck_delete_title)
                .setMessage(getString(R.string.deck_delete_body, name))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.deleteDeck();
                    navController.navigateUp();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.noteList.setAdapter(null);
        binding = null;
    }
}

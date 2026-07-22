package com.mowtiie.flashback.ui.decks;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mowtiie.flashback.MainActivity;
import com.mowtiie.flashback.ui.AppBarLift;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.mowtiie.flashback.data.io.ArchiveFiles;
import com.mowtiie.flashback.data.io.ArchiveSerializer;
import com.mowtiie.flashback.AppExecutors;

import java.io.IOException;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.databinding.FragmentDeckDetailBinding;

import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mowtiie.flashback.repository.FlashbackRepository;
import com.mowtiie.flashback.util.ViewModelFactory;

public class DeckDetailFragment extends Fragment {

    private FragmentDeckDetailBinding binding;
    private DeckDetailViewModel viewModel;
    private NoteAdapter adapter;

    private ActivityResultLauncher<String> createDocument;
    private String pendingExportContent;

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

        createDocument = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                    if (uri != null && pendingExportContent != null) {
                        writeExport(uri, pendingExportContent);
                    }
                    pendingExportContent = null;
                });

        long deckId = requireArguments().getLong("deckId");
        viewModel = new ViewModelProvider(this, new ViewModelFactory(
                () -> new DeckDetailViewModel(requireActivity().getApplication(), deckId)))
                .get(DeckDetailViewModel.class);

        NavController navController = NavHostFragment.findNavController(this);

        adapter = new NoteAdapter(note -> {
            Bundle args = new Bundle();
            args.putLong("deckId", deckId);
            args.putLong("noteId", note.id);
            navController.navigate(R.id.action_deckDetail_to_noteEditor, args);
        });

        binding.noteList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.noteList.setAdapter(adapter);

        if (requireActivity() instanceof MainActivity) {
            AppBarLift.attach(((MainActivity) requireActivity()).getAppBar(), binding.noteList);
        }

        binding.studyDeck.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("deckId", deckId);
            navController.navigate(R.id.action_deckDetail_to_study, args);
        });

        binding.emptyState.emptyTitle.setText(R.string.deck_detail_empty_title);
        binding.emptyState.emptyBody.setText(R.string.deck_detail_empty_body);
        binding.emptyState.emptyAction.setText(R.string.deck_detail_empty_action);
        binding.emptyState.emptyAction.setOnClickListener(
                v -> openNewNote(navController, deckId));

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_deck_detail, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_add_note) {
                    openNewNote(navController, deckId);
                    return true;
                }
                if (id == R.id.action_edit_deck) {
                    Bundle args = new Bundle();
                    args.putLong("deckId", deckId);
                    navController.navigate(R.id.action_deckDetail_to_deckEditor, args);
                    return true;
                }
                if (id == R.id.action_export_deck) {
                    exportDeck(deckId);
                    return true;
                }
                if (id == R.id.action_delete_deck) {
                    confirmDeleteDeck(navController);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        viewModel.getDeck().observe(getViewLifecycleOwner(), deck -> {
            if (deck != null) {
                requireActivity().setTitle(deck.name);
            }
        });

        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            boolean empty = notes == null || notes.isEmpty();
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.noteList.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        // The FAB offers studying only when there is something to study. An
        // empty deck, or one with nothing due, leaves adding cards in the
        // toolbar as the available action.
        viewModel.getSummary().observe(getViewLifecycleOwner(), summary -> {
            int studyable = summary == null ? 0 : summary.studyableCount();
            binding.studyDeck.setVisibility(studyable > 0 ? View.VISIBLE : View.GONE);
            binding.studyDeck.setText(getString(R.string.deck_study_button, studyable));
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

    /**
     * Serializes just this deck, then opens the save picker. Reuses the same
     * serializer and file writer as the collection-wide export in Settings.
     */
    private void exportDeck(long deckId) {
        FlashbackRepository.getInstance(requireContext().getApplicationContext())
                .exportDeck(deckId, archive -> {
                    if (archive.decks.isEmpty()) {
                        return;
                    }
                    pendingExportContent = new ArchiveSerializer().serialize(archive);
                    String safe = archive.decks.get(0).name
                            .replaceAll("[^a-zA-Z0-9-_]", "_");
                    createDocument.launch(getString(
                            R.string.export_deck_filename_prefix) + safe + ".json");
                });
    }

    private void writeExport(Uri destination, String content) {
        AppExecutors executors = AppExecutors.getInstance();
        executors.diskIO().execute(() -> {
            try {
                ArchiveFiles.write(requireContext().getContentResolver(),
                        destination, content);
                executors.mainThread().execute(() -> {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), R.string.deck_export,
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                // A failed write leaves the collection untouched; nothing to undo.
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.noteList.setAdapter(null);
        binding = null;
    }
}

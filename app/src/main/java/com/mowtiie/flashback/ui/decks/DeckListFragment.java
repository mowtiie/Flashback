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

import com.mowtiie.flashback.R;
import com.mowtiie.flashback.databinding.FragmentDeckListBinding;

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

        adapter = new DeckAdapter(summary -> {
            Bundle args = new Bundle();
            args.putLong("deckId", summary.deck.id);
            navController.navigate(R.id.action_deckList_to_deckDetail, args);
        });

        binding.deckList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.deckList.setAdapter(adapter);

        binding.addDeck.setOnClickListener(v ->
                navController.navigate(R.id.action_deckList_to_deckEditor));

        binding.emptyState.emptyTitle.setText(R.string.decks_empty_title);
        binding.emptyState.emptyBody.setText(R.string.decks_empty_body);
        binding.emptyState.emptyAction.setText(R.string.decks_empty_action);
        binding.emptyState.emptyAction.setOnClickListener(v -> navController.navigate(R.id.action_deckList_to_deckEditor));

        viewModel.getSummaries().observe(getViewLifecycleOwner(), summaries -> {
            adapter.submitList(summaries);
            boolean empty = summaries == null || summaries.isEmpty();
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.deckList.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.addDeck.setVisibility(empty ? View.GONE : View.VISIBLE);
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
        binding.deckList.setAdapter(null);
        binding = null;
    }
}

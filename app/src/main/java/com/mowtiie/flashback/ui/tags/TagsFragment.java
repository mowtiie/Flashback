package com.mowtiie.flashback.ui.tags;

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
import com.mowtiie.flashback.databinding.FragmentTagsBinding;

public class TagsFragment extends Fragment {

    private FragmentTagsBinding binding;
    private TagsViewModel viewModel;
    private TagAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTagsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TagsViewModel.class);
        NavController navController = NavHostFragment.findNavController(this);

        adapter = new TagAdapter(tag -> {
            Bundle args = new Bundle();
            args.putLong("tagId", tag.tag.id);
            navController.navigate(R.id.action_tags_to_tagEditor, args);
        });

        binding.tagList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.tagList.setAdapter(adapter);

        binding.addTag.setOnClickListener(v ->
                navController.navigate(R.id.action_tags_to_tagEditor));

        binding.emptyState.emptyIcon.setImageResource(R.drawable.ic_tags);
        binding.emptyState.emptyTitle.setText(R.string.tags_empty_title);
        binding.emptyState.emptyBody.setText(R.string.tags_empty_body);
        binding.emptyState.emptyAction.setText(R.string.tag_add);
        binding.emptyState.emptyAction.setOnClickListener(v ->
                navController.navigate(R.id.action_tags_to_tagEditor));

        viewModel.getTags().observe(getViewLifecycleOwner(), tags -> {
            adapter.submitList(tags);
            boolean empty = tags == null || tags.isEmpty();
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.tagList.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.addTag.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.tagList.setAdapter(null);
        binding = null;
    }
}

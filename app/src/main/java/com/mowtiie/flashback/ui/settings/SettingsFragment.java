package com.mowtiie.flashback.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mowtiie.flashback.R;
import com.mowtiie.flashback.databinding.FragmentPlaceholderBinding;

public class SettingsFragment extends Fragment {

    private FragmentPlaceholderBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setTitle(R.string.settings_title);
        binding.emptyState.getRoot().setVisibility(View.VISIBLE);
        binding.emptyState.emptyIcon.setImageResource(R.drawable.ic_settings);
        binding.emptyState.emptyTitle.setText(R.string.settings_coming_title);
        binding.emptyState.emptyBody.setText(R.string.settings_coming_body);
        binding.emptyState.emptyAction.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

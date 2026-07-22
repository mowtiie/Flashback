package com.mowtiie.flashback.ui.study;

import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.databinding.FragmentStudyBinding;
import com.mowtiie.flashback.scheduler.Rating;
import com.mowtiie.flashback.util.ViewModelFactory;

import java.util.concurrent.TimeUnit;

public class StudyFragment extends Fragment {

    private static final long FLIP_HALF_MS = 140L;

    private FragmentStudyBinding binding;
    private StudyViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        long deckId = requireArguments().getLong("deckId");
        viewModel = new ViewModelProvider(this, new ViewModelFactory(
                () -> new StudyViewModel(requireActivity().getApplication(), deckId)))
                .get(StudyViewModel.class);

        NavController navController = NavHostFragment.findNavController(this);
        binding.toolbar.setTitle(R.string.study_title);
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        binding.toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_undo) {
                viewModel.undo();
                Snackbar.make(binding.getRoot(), R.string.study_undone,
                        Snackbar.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        binding.showAnswer.setOnClickListener(v -> revealWithFlip());
        binding.ratingAgain.setOnClickListener(v -> viewModel.answer(Rating.AGAIN));
        binding.ratingHard.setOnClickListener(v -> viewModel.answer(Rating.HARD));
        binding.ratingGood.setOnClickListener(v -> viewModel.answer(Rating.GOOD));
        binding.ratingEasy.setOnClickListener(v -> viewModel.answer(Rating.EASY));

        binding.emptyState.emptyAction.setText(R.string.study_back_to_deck);
        binding.emptyState.emptyAction.setOnClickListener(v -> navController.navigateUp());

        observe(navController);
    }

    private void observe(NavController navController) {
        viewModel.getCurrent().observe(getViewLifecycleOwner(), card -> {
            boolean hasCard = card != null;
            binding.studyContent.setVisibility(hasCard ? View.VISIBLE : View.GONE);
            binding.countsRow.setVisibility(hasCard ? View.VISIBLE : View.GONE);
            if (hasCard) {
                binding.questionText.setText(card.question());
                binding.answerText.setText(card.answer());
            }
        });

        viewModel.getRevealed().observe(getViewLifecycleOwner(), revealed -> {
            boolean shown = Boolean.TRUE.equals(revealed);
            binding.answerText.setVisibility(shown ? View.VISIBLE : View.GONE);
            binding.answerDivider.setVisibility(shown ? View.VISIBLE : View.GONE);
            binding.showAnswer.setVisibility(shown ? View.GONE : View.VISIBLE);
            binding.ratingRow.setVisibility(shown ? View.VISIBLE : View.GONE);
        });

        viewModel.getCounts().observe(getViewLifecycleOwner(), counts -> {
            if (counts == null) {
                return;
            }
            binding.newRemaining.setText(String.valueOf(counts.newCards));
            binding.learningRemaining.setText(String.valueOf(counts.learning));
            binding.dueRemaining.setText(String.valueOf(counts.due));
        });

        viewModel.getPreviews().observe(getViewLifecycleOwner(), previews -> {
            if (previews == null) {
                return;
            }
            binding.intervalAgain.setText(previews.get(Rating.AGAIN));
            binding.intervalHard.setText(previews.get(Rating.HARD));
            binding.intervalGood.setText(previews.get(Rating.GOOD));
            binding.intervalEasy.setText(previews.get(Rating.EASY));
        });

        viewModel.getCanUndo().observe(getViewLifecycleOwner(), canUndo ->
                binding.toolbar.getMenu().findItem(R.id.action_undo)
                        .setVisible(Boolean.TRUE.equals(canUndo)));

        viewModel.getEnding().observe(getViewLifecycleOwner(), this::renderEnding);
    }

    private void renderEnding(StudyViewModel.Ending ending) {
        if (ending == null) {
            binding.emptyState.getRoot().setVisibility(View.GONE);
            return;
        }

        binding.emptyState.getRoot().setVisibility(View.VISIBLE);
        binding.studyContent.setVisibility(View.GONE);
        binding.countsRow.setVisibility(View.GONE);

        switch (ending) {
            case NOTHING_DUE:
                binding.emptyState.emptyTitle.setText(R.string.study_nothing_due_title);
                binding.emptyState.emptyBody.setText(R.string.study_nothing_due_body);
                break;
            case FINISHED:
                int answered = viewModel.getAnsweredThisSession();
                binding.emptyState.emptyTitle.setText(R.string.study_finished_title);
                binding.emptyState.emptyBody.setText(getResources().getQuantityString(
                        R.plurals.study_finished_body, answered, answered));
                break;
            case WAITING:
                Long dueAt = viewModel.getWaitingUntil().getValue();
                long minutes = dueAt == null ? 1 : Math.max(1, TimeUnit.MILLISECONDS.toMinutes(
                        dueAt - System.currentTimeMillis()));
                binding.emptyState.emptyTitle.setText(R.string.study_waiting_title);
                binding.emptyState.emptyBody.setText(getString(R.string.study_waiting_body,
                        getString(R.string.minutes_short, minutes)));
                break;
            default:
                break;
        }
    }

    /**
     * Half a flip: rotate to edge-on, swap the face, then rotate back from the
     * opposite edge. Rotating a full 180 degrees would leave the answer text
     * mirrored.
     */
    private void revealWithFlip() {
        if (!animationsEnabled()) {
            viewModel.reveal();
            return;
        }

        View face = binding.cardFace;
        face.setCameraDistance(8000 * getResources().getDisplayMetrics().density);
        face.animate()
                .rotationY(90f)
                .setDuration(FLIP_HALF_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (binding == null) {
                        return;
                    }
                    viewModel.reveal();
                    face.setRotationY(-90f);
                    face.animate()
                            .rotationY(0f)
                            .setDuration(FLIP_HALF_MS)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    /** Honours the system "remove animations" accessibility setting. */
    private boolean animationsEnabled() {
        float scale = Settings.Global.getFloat(
                requireContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale != 0f;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.cardFace.animate().cancel();
        binding = null;
    }
}

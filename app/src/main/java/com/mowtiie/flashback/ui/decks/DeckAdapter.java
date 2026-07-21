package com.mowtiie.flashback.ui.decks;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.data.model.DeckSummary;
import com.mowtiie.flashback.databinding.ItemDeckBinding;

public class DeckAdapter extends ListAdapter<DeckSummary, DeckAdapter.DeckViewHolder> {

    public interface OnDeckClick {
        void onDeckClick(DeckSummary summary);
    }

    private final OnDeckClick clickListener;

    public DeckAdapter(OnDeckClick clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<DeckSummary> DIFF =
            new DiffUtil.ItemCallback<DeckSummary>() {

                @Override
                public boolean areItemsTheSame(@NonNull DeckSummary a, @NonNull DeckSummary b) {
                    return a.deck.id == b.deck.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DeckSummary a, @NonNull DeckSummary b) {
                    return a.deck.name.equals(b.deck.name)
                            && equalsNullable(a.deck.description, b.deck.description)
                            && a.newCount == b.newCount
                            && a.learnCount == b.learnCount
                            && a.dueCount == b.dueCount
                            && a.totalCount == b.totalCount;
                }

                private boolean equalsNullable(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeckBinding binding = ItemDeckBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DeckViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class DeckViewHolder extends RecyclerView.ViewHolder {

        private final ItemDeckBinding binding;

        DeckViewHolder(ItemDeckBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DeckSummary summary, OnDeckClick listener) {
            binding.deckName.setText(summary.deck.name);

            boolean hasDescription = summary.deck.description != null
                    && !summary.deck.description.trim().isEmpty();
            binding.deckDescription.setVisibility(hasDescription ? android.view.View.VISIBLE
                    : android.view.View.GONE);
            binding.deckDescription.setText(summary.deck.description);

            int availableNew = Math.min(summary.newCount, summary.deck.newPerDay);

            applyCount(binding.newCount, availableNew, R.color.count_new);
            applyCount(binding.learningCount, summary.learnCount, R.color.count_learning);
            applyCount(binding.dueCount, summary.dueCount, R.color.count_due);

            binding.deckTotal.setText(binding.getRoot().getContext()
                    .getString(R.string.deck_card_count, summary.totalCount));

            binding.getRoot().setOnClickListener(v -> listener.onDeckClick(summary));
        }

        private void applyCount(TextView view, int count, int colorRes) {
            view.setText(String.valueOf(count));
            int color = count > 0
                    ? ContextCompat.getColor(view.getContext(), colorRes)
                    : MaterialColors.getColor(view,
                            com.google.android.material.R.attr.colorOnSurfaceVariant);
            view.setTextColor(color);
        }
    }
}

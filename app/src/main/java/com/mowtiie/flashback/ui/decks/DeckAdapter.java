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
import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.data.model.DeckListItem;
import com.mowtiie.flashback.util.TagChips;
import com.mowtiie.flashback.databinding.ItemDeckBinding;

import com.google.android.material.chip.Chip;

public class DeckAdapter extends ListAdapter<DeckListItem, DeckAdapter.DeckViewHolder> {

    public interface OnDeckClick {
        void onDeckClick(DeckListItem summary);
    }

    private final OnDeckClick clickListener;

    public DeckAdapter(OnDeckClick clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<DeckListItem> DIFF =
            new DiffUtil.ItemCallback<DeckListItem>() {

                @Override
                public boolean areItemsTheSame(@NonNull DeckListItem a, @NonNull DeckListItem b) {
                    return a.summary.deck.id == b.summary.deck.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DeckListItem a, @NonNull DeckListItem b) {
                    return a.summary.deck.name.equals(b.summary.deck.name)
                            && equalsNullable(a.summary.deck.description,
                            b.summary.deck.description)
                            && a.summary.newCount == b.summary.newCount
                            && a.summary.learnCount == b.summary.learnCount
                            && a.summary.dueCount == b.summary.dueCount
                            && a.summary.totalCount == b.summary.totalCount
                            && sameTags(a.tags, b.tags);
                }

                /** Cheap identity comparison; tag edits change the id list rarely. */
                private boolean sameTags(java.util.List<Tag> a, java.util.List<Tag> b) {
                    if (a.size() != b.size()) {
                        return false;
                    }
                    for (int i = 0; i < a.size(); i++) {
                        if (a.get(i).id != b.get(i).id
                                || a.get(i).color != b.get(i).color
                                || !a.get(i).name.equals(b.get(i).name)) {
                            return false;
                        }
                    }
                    return true;
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

        void bind(DeckListItem item, OnDeckClick listener) {
            binding.deckName.setText(item.summary.deck.name);

            boolean hasDescription = item.summary.deck.description != null
                    && !item.summary.deck.description.trim().isEmpty();
            binding.deckDescription.setVisibility(hasDescription ? android.view.View.VISIBLE
                    : android.view.View.GONE);
            binding.deckDescription.setText(item.summary.deck.description);

            bindTags(item.tags);

            // New cards are capped per day, so show what is actually available
            // today rather than the whole unseen pile.
            int availableNew = Math.min(item.summary.newCount, item.summary.deck.newPerDay);

            applyCount(binding.newCount, availableNew, R.color.count_new);
            applyCount(binding.learningCount, item.summary.learnCount, R.color.count_learning);
            applyCount(binding.dueCount, item.summary.dueCount, R.color.count_due);

            binding.deckTotal.setText(binding.getRoot().getContext()
                    .getString(R.string.deck_card_count, item.summary.totalCount));

            binding.getRoot().setOnClickListener(v -> listener.onDeckClick(item));
        }

        /** Chips are rebuilt per bind; a deck carries few enough tags for this to be free. */
        private void bindTags(java.util.List<Tag> tags) {
            binding.deckTags.removeAllViews();
            binding.deckTags.setVisibility(tags.isEmpty()
                    ? android.view.View.GONE : android.view.View.VISIBLE);

            for (Tag tag : tags) {
                Chip chip = new Chip(binding.getRoot().getContext());
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setEnsureMinTouchTargetSize(false);
                chip.setChipMinHeight(binding.getRoot().getResources()
                        .getDimension(R.dimen.chip_compact_height));
                TagChips.apply(chip, tag);
                binding.deckTags.addView(chip);
            }
        }

        /**
         * A zero drops to the muted surface colour instead of keeping its queue
         * colour, so a glance down the list picks out only decks with work in
         * them.
         */
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

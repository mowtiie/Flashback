package com.mowtiie.flashback.ui.tags;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.mowtiie.flashback.R;
import com.mowtiie.flashback.data.model.TagWithCount;
import com.mowtiie.flashback.databinding.ItemTagBinding;

public class TagAdapter extends ListAdapter<TagWithCount, TagAdapter.TagViewHolder> {

    public interface OnTagClick {
        void onTagClick(TagWithCount tag);
    }

    private final OnTagClick clickListener;

    public TagAdapter(OnTagClick clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<TagWithCount> DIFF =
            new DiffUtil.ItemCallback<TagWithCount>() {

                @Override
                public boolean areItemsTheSame(@NonNull TagWithCount a, @NonNull TagWithCount b) {
                    return a.tag.id == b.tag.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull TagWithCount a,
                                                  @NonNull TagWithCount b) {
                    return a.tag.name.equals(b.tag.name)
                            && a.tag.color == b.tag.color
                            && a.deckCount == b.deckCount
                            && (a.tag.description == null
                            ? b.tag.description == null
                            : a.tag.description.equals(b.tag.description));
                }
            };

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TagViewHolder(ItemTagBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {

        private final ItemTagBinding binding;

        TagViewHolder(ItemTagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TagWithCount item, OnTagClick listener) {
            binding.tagName.setText(item.tag.name);

            int colour = item.tag.color == 0
                    ? MaterialColors.getColor(binding.getRoot(),
                    com.google.android.material.R.attr.colorOutline)
                    : item.tag.color;
            binding.tagSwatch.setBackgroundTintList(ColorStateList.valueOf(colour));

            boolean hasDescription = item.tag.description != null
                    && !item.tag.description.trim().isEmpty();
            binding.tagDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
            binding.tagDescription.setText(item.tag.description);

            binding.tagDeckCount.setText(item.deckCount == 0
                    ? binding.getRoot().getContext().getString(R.string.tag_unused)
                    : binding.getRoot().getResources().getQuantityString(
                    R.plurals.tag_deck_count, item.deckCount, item.deckCount));

            binding.getRoot().setOnClickListener(v -> listener.onTagClick(item));
        }
    }
}

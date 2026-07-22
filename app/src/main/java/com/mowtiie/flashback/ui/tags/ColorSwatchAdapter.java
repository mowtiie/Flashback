package com.mowtiie.flashback.ui.tags;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.flashback.databinding.ItemColorSwatchBinding;

/** The colour grid in the tag editor. */
public class ColorSwatchAdapter
        extends RecyclerView.Adapter<ColorSwatchAdapter.SwatchViewHolder> {

    public interface OnColorSelected {
        void onColorSelected(int color);
    }

    private final int[] colors;
    private final OnColorSelected listener;
    private int selected;

    public ColorSwatchAdapter(int[] colors, int selected, OnColorSelected listener) {
        this.colors = colors;
        this.selected = selected;
        this.listener = listener;
    }

    public void setSelected(int color) {
        int previous = indexOf(selected);
        selected = color;
        if (previous >= 0) {
            notifyItemChanged(previous);
        }
        int now = indexOf(color);
        if (now >= 0) {
            notifyItemChanged(now);
        }
    }

    private int indexOf(int color) {
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == color) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public SwatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SwatchViewHolder(ItemColorSwatchBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SwatchViewHolder holder, int position) {
        holder.bind(colors[position], colors[position] == selected, listener);
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    static class SwatchViewHolder extends RecyclerView.ViewHolder {

        private final ItemColorSwatchBinding binding;

        SwatchViewHolder(ItemColorSwatchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(int color, boolean isSelected, OnColorSelected listener) {
            binding.swatch.setBackgroundTintList(ColorStateList.valueOf(color));
            binding.swatchCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            // Tick contrasts against the swatch it sits on, not the theme.
            binding.swatchCheck.setImageTintList(ColorStateList.valueOf(
                    ColorUtils.calculateLuminance(color) > 0.5d ? Color.BLACK : Color.WHITE));
            binding.getRoot().setOnClickListener(v -> listener.onColorSelected(color));
        }
    }
}

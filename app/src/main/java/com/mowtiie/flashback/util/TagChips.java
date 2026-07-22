package com.mowtiie.flashback.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.mowtiie.flashback.data.entity.Tag;

public final class TagChips {

    private static final float FILL_ALPHA_LIGHT = 0.14f;
    private static final float FILL_ALPHA_DARK = 0.26f;

    private TagChips() {
    }

    public static void apply(Chip chip, Tag tag) {
        chip.setText(tag.name);

        int surface = MaterialColors.getColor(chip,
                com.google.android.material.R.attr.colorSurface);
        int onSurface = MaterialColors.getColor(chip,
                com.google.android.material.R.attr.colorOnSurface);

        int accent = tag.color == 0 ? onSurface : tag.color;
        float alpha = isDark(surface) ? FILL_ALPHA_DARK : FILL_ALPHA_LIGHT;

        chip.setChipBackgroundColor(ColorStateList.valueOf(
                ColorUtils.blendARGB(surface, accent, alpha)));
        chip.setChipStrokeColor(ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(accent, 140)));
        chip.setChipStrokeWidth(1f);
        chip.setTextColor(onSurface);
    }

    private static boolean isDark(int surface) {
        return ColorUtils.calculateLuminance(surface) < 0.5d;
    }

    public static int[] palette(Context context) {
        int[] colors;
        try (android.content.res.TypedArray array = context.getResources().obtainTypedArray(com.mowtiie.flashback.R.array.tag_palette)) {
            colors = new int[array.length()];
            for (int i = 0; i < array.length(); i++) {
                colors[i] = array.getColor(i, Color.GRAY);
            }
            array.recycle();
        }
        return colors;
    }
}

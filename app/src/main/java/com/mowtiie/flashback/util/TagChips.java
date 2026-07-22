package com.mowtiie.flashback.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.mowtiie.flashback.data.entity.Tag;

/**
 * Styles a chip from a tag's colour.
 *
 * <p>The tag colour is used as the outline and as a low-opacity wash over the
 * surface, while the label keeps the theme's onSurface colour. Painting the
 * whole chip in the tag colour would mean computing a readable text colour for
 * twelve hues against two themes, and getting it wrong somewhere. This way the
 * tag stays recognisable and the text is always legible.
 */
public final class TagChips {

    /** Wash strength. Dark surfaces need a touch more to read as tinted. */
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

    /** Reads the palette a tag colour is chosen from. */
    public static int[] palette(Context context) {
        android.content.res.TypedArray array =
                context.getResources().obtainTypedArray(
                        com.mowtiie.flashback.R.array.tag_palette);
        int[] colors = new int[array.length()];
        for (int i = 0; i < array.length(); i++) {
            colors[i] = array.getColor(i, Color.GRAY);
        }
        array.recycle();
        return colors;
    }
}

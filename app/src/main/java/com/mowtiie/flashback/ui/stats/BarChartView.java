package com.mowtiie.flashback.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

/**
 * A minimal vertical bar chart drawn straight onto the canvas. Deliberately not
 * a charting library: the project carries no such dependency, and the activity
 * chart needs only scaled bars with a few labels. Colours are resolved from the
 * theme so it follows light and dark automatically.
 */
public class BarChartView extends View {

    private static final int BAR_CORNER_DP = 3;
    private static final int LABEL_SIZE_SP = 11;
    private static final int LABEL_GAP_DP = 6;
    private static final int MIN_BAR_HEIGHT_DP = 2;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private int[] values = new int[0];
    private String[] labels = new String[0];

    private final float density;
    private final float cornerPx;
    private final float labelGapPx;
    private final float minBarPx;

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        cornerPx = BAR_CORNER_DP * density;
        labelGapPx = LABEL_GAP_DP * density;
        minBarPx = MIN_BAR_HEIGHT_DP * density;

        int primary = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorPrimaryFixed);
        int track = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceVariant);
        int onSurfaceVariant = MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant);

        barPaint.setColor(primary);
        trackPaint.setColor(track);
        labelPaint.setColor(onSurfaceVariant);
        labelPaint.setTextSize(LABEL_SIZE_SP * getResources().getDisplayMetrics().scaledDensity);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int[] values, String[] labels) {
        this.values = values == null ? new int[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.length == 0) {
            return;
        }

        float labelHeight = labelPaint.getTextSize() + labelGapPx;
        float chartHeight = getHeight() - getPaddingTop() - getPaddingBottom() - labelHeight;
        float usableWidth = getWidth() - getPaddingStart() - getPaddingEnd();
        float slot = usableWidth / values.length;
        // Bars take part of each slot, leaving gaps between them.
        float barWidth = slot * 0.6f;
        float baseline = getPaddingTop() + chartHeight;

        int max = 1;
        for (int value : values) {
            max = Math.max(max, value);
        }

        for (int i = 0; i < values.length; i++) {
            float centerX = getPaddingStart() + slot * i + slot / 2f;
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;

            // A faint full-height track keeps the grid legible on quiet days.
            barRect.set(left, getPaddingTop(), right, baseline);
            canvas.drawRoundRect(barRect, cornerPx, cornerPx, trackPaint);

            if (values[i] > 0) {
                float barHeight = Math.max(minBarPx, chartHeight * values[i] / max);
                barRect.set(left, baseline - barHeight, right, baseline);
                canvas.drawRoundRect(barRect, cornerPx, cornerPx, barPaint);
            }

            if (i < labels.length && labels[i] != null && !labels[i].isEmpty()) {
                canvas.drawText(labels[i], centerX,
                        baseline + labelHeight - labelGapPx / 2f, labelPaint);
            }
        }
    }
}

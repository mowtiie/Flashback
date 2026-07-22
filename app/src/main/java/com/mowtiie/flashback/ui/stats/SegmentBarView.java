package com.mowtiie.flashback.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * A single horizontal bar split into proportional coloured segments, for the
 * new / learning / young / mature breakdown. A stacked bar reads the makeup of
 * the collection at a glance far better than four separate numbers.
 */
public class SegmentBarView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float radiusPx;

    private int[] values = new int[0];
    private int[] colors = new int[0];

    public SegmentBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        radiusPx = 6 * getResources().getDisplayMetrics().density;
    }

    public void setSegments(int[] values, int[] colors) {
        this.values = values == null ? new int[0] : values;
        this.colors = colors == null ? new int[0] : colors;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int total = 0;
        for (int value : values) {
            total += value;
        }
        float width = getWidth();
        float height = getHeight();

        if (total == 0) {
            // Nothing to show yet: draw an empty rounded track.
            paint.setColor(0x22888888);
            rect.set(0, 0, width, height);
            canvas.drawRoundRect(rect, radiusPx, radiusPx, paint);
            return;
        }

        float x = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) {
                continue;
            }
            float segmentWidth = width * values[i] / total;
            paint.setColor(colors.length > i ? colors[i] : 0xFF888888);
            rect.set(x, 0, x + segmentWidth, height);
            canvas.drawRect(rect, paint);
            x += segmentWidth;
        }

        // Round the whole bar by masking is overkill; the small radius on a
        // thin bar reads fine with square internal joins.
    }
}

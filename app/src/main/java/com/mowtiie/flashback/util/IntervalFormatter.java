package com.mowtiie.flashback.util;

import android.content.Context;

import com.mowtiie.flashback.R;
import com.mowtiie.flashback.scheduler.CardState;
import com.mowtiie.flashback.scheduler.SchedulingState;

import java.util.Locale;

public final class IntervalFormatter {

    private static final long MINUTE_MS = 60_000L;

    private IntervalFormatter() {
    }

    public static String format(Context context, SchedulingState state, long now) {
        if (CardState.isLearningLike(state.state)) {
            long minutes = Math.max(1, (state.dueAt - now) / MINUTE_MS);
            if (minutes < 60) {
                return context.getString(R.string.interval_minutes, minutes);
            }
            return context.getString(R.string.interval_hours, Math.round(minutes / 60d));
        }

        int days = state.intervalDays;
        if (days < 30) {
            return context.getString(R.string.interval_days, days);
        }
        if (days < 365) {
            return context.getString(R.string.interval_months,
                    round1(days / 30.4d));
        }
        return context.getString(R.string.interval_years, round1(days / 365d));
    }

    private static String round1(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }
}

package com.mowtiie.flashback.ui;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

/**
 * Drives the shared app bar's lifted state from a fragment's own scrolling view.
 *
 * <p>The standard {@code liftOnScroll} mechanism can't be used with a single
 * activity-level app bar, because it requires the scrolling view to be a sibling
 * of the {@link AppBarLayout} inside the same CoordinatorLayout. Here the
 * scroller lives inside a swapped-in fragment, two levels down, so instead each
 * scrolling fragment attaches its scroller here and we toggle
 * {@link AppBarLayout#setLifted} as it moves off the top.
 *
 * <p>The listeners are bound to the view, so they are released when the
 * fragment's view is destroyed; no explicit teardown is needed.
 */
public final class AppBarLift {

    private AppBarLift() {
    }

    public static void attach(AppBarLayout appBar, @NonNull RecyclerView recyclerView) {
        if (appBar == null) {
            return;
        }
        appBar.setLifted(recyclerView.canScrollVertically(-1));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                appBar.setLifted(rv.canScrollVertically(-1));
            }
        });
    }

    public static void attach(AppBarLayout appBar, @NonNull NestedScrollView scrollView) {
        if (appBar == null) {
            return;
        }
        appBar.setLifted(scrollView.canScrollVertically(-1));
        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, x, y, oldX, oldY) ->
                        appBar.setLifted(v.canScrollVertically(-1)));
    }
}

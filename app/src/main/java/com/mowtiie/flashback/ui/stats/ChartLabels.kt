package com.mowtiie.flashback.ui.stats

/** Decides which axis labels fit, so large text never makes neighbours run together. */
object ChartLabels {
    /** [widths] has one entry per slot; null means no label there. A label is dropped if it would touch the last shown one. */
    fun visible(widths: List<Float?>, slot: Float, gap: Float): List<Boolean> {
        var lastRight = Float.NEGATIVE_INFINITY
        return widths.mapIndexed { index, width ->
            if (width == null) return@mapIndexed false
            val left = slot * index + (slot - width) / 2f
            if (left < lastRight + gap) return@mapIndexed false
            lastRight = left + width
            true
        }
    }
}

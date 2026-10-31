package com.mowtiie.flashback.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartLabelsTest {

    @Test
    fun labelsThatFitAreAllShown() {
        val shown = ChartLabels.visible(widths = listOf(20f, null, null, 20f), slot = 30f, gap = 4f)
        assertEquals(listOf(true, false, false, true), shown)
    }

    @Test
    fun anEmptyLabelIsNeverShown() {
        assertEquals(listOf(false, false), ChartLabels.visible(listOf(null, null), 30f, 4f))
    }

    @Test
    fun aLabelTouchingTheLastShownOneIsSkipped() {
        // Slots 30 wide, labels 40 wide: neighbours overlap, so every other one drops out.
        val shown = ChartLabels.visible(widths = listOf(40f, 40f, 40f, 40f), slot = 30f, gap = 4f)
        assertEquals(listOf(true, false, true, false), shown)
    }

    @Test
    fun theGapBetweenLabelsIsKept() {
        // Edges are 2 apart, under the 4 gap, so the second is skipped.
        assertEquals(listOf(true, false), ChartLabels.visible(listOf(28f, 28f), 30f, 4f))
        // Edges are 6 apart, so both fit.
        assertEquals(listOf(true, true), ChartLabels.visible(listOf(24f, 24f), 30f, 4f))
    }

    @Test
    fun theFirstLabelIsAlwaysKept() {
        assertEquals(true, ChartLabels.visible(listOf(500f), 30f, 4f).single())
    }
}

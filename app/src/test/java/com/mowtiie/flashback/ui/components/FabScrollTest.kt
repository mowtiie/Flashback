package com.mowtiie.flashback.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FabScrollTest {

    private fun at(index: Int, offset: Int = 0) = ScrollPosition(index, offset)

    @Test
    fun atTheTopItIsAlwaysExpanded() {
        assertTrue(FabScroll.expanded(previous = at(2), current = at(0), wasExpanded = false))
        assertTrue(FabScroll.expanded(previous = at(0, 10), current = at(0, 0), wasExpanded = false))
    }

    @Test
    fun scrollingDownCollapsesIt() {
        assertFalse(FabScroll.expanded(previous = at(0, 0), current = at(0, 40), wasExpanded = true))
        assertFalse(FabScroll.expanded(previous = at(1, 300), current = at(2, 0), wasExpanded = true))
    }

    @Test
    fun scrollingUpExpandsIt() {
        assertTrue(FabScroll.expanded(previous = at(3, 0), current = at(2, 100), wasExpanded = false))
        assertTrue(FabScroll.expanded(previous = at(1, 80), current = at(1, 20), wasExpanded = false))
    }

    @Test
    fun noMovementKeepsTheCurrentState() {
        assertTrue(FabScroll.expanded(previous = at(1, 5), current = at(1, 5), wasExpanded = true))
        assertFalse(FabScroll.expanded(previous = at(1, 5), current = at(1, 5), wasExpanded = false))
    }
}

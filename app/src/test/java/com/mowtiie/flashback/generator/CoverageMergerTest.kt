package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverageMergerTest {

    private fun deck(name: String, vararg fronts: String) =
        DeckArchive.Deck(name = name, notes = fronts.map { DeckArchive.Note(it, "a-$it") })

    @Test
    fun sectionsAreJoinedInOrderWithTheFirstSectionsName() {
        val merged = CoverageMerger.merge(listOf(deck("Cells", "A", "B"), deck("Other", "C")))

        assertEquals("Cells", merged.name)
        assertEquals(listOf("A", "B", "C"), merged.notes.map { it.front })
        assertEquals(listOf(2, 1), merged.perSection)
    }

    @Test
    fun repeatedFrontsAcrossSectionsKeepTheFirstOne() {
        val merged = CoverageMerger.merge(listOf(deck("D", "What is DNA?", "B"), deck("D", "what is dna? ", "C")))

        assertEquals(listOf("What is DNA?", "B", "C"), merged.notes.map { it.front })
        assertEquals(listOf(2, 1), merged.perSection)
    }

    @Test
    fun aMissingSectionIsSkippedButKeepsItsPlaceInTheCounts() {
        val merged = CoverageMerger.merge(listOf(deck("D", "A"), null, deck("D", "C")))

        assertEquals(listOf("A", "C"), merged.notes.map { it.front })
        assertEquals(listOf(1, 0, 1), merged.perSection)
        assertEquals("D", merged.name)
    }

    @Test
    fun nameComesFromTheFirstSectionThatSucceeded() {
        val merged = CoverageMerger.merge(listOf(null, deck("Second", "A")))
        assertEquals("Second", merged.name)
    }

    @Test
    fun nothingMadeGivesAnEmptyResult() {
        val merged = CoverageMerger.merge(listOf(null, null))
        assertTrue(merged.notes.isEmpty())
        assertEquals("", merged.name)
        assertEquals(listOf(0, 0), merged.perSection)
    }
}

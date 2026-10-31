package com.mowtiie.flashback.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialSplitterTest {

    private fun squash(text: String) = text.filterNot { it.isWhitespace() }

    private fun paragraphs(count: Int, size: Int) =
        (1..count).joinToString("\n\n") { n -> "P$n " + "word ".repeat(size / 5).trim() }

    @Test
    fun shortTextIsOneSection() {
        val parts = MaterialSplitter.split("A short note.", target = 100)
        assertEquals(listOf("A short note."), parts)
    }

    @Test
    fun emptyAndBlankTextGiveNoSections() {
        assertTrue(MaterialSplitter.split("", 100).isEmpty())
        assertTrue(MaterialSplitter.split("  \n\n \t ", 100).isEmpty())
    }

    @Test
    fun splitsAtParagraphBoundariesAndKeepsSectionsNearTheTarget() {
        val text = paragraphs(count = 10, size = 100)
        val parts = MaterialSplitter.split(text, target = 250)

        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 250 })
        // Every section starts at a paragraph start, so no paragraph is cut.
        assertTrue(parts.all { it.startsWith("P") })
    }

    @Test
    fun noTextIsLostOrReordered() {
        val text = paragraphs(count = 25, size = 120)
        val parts = MaterialSplitter.split(text, target = 300)
        assertEquals(squash(text), squash(parts.joinToString("")))
    }

    @Test
    fun aHugeParagraphFallsBackToSentencesThenToWords() {
        val sentences = (1..40).joinToString(" ") { "Sentence number $it is here." }
        val parts = MaterialSplitter.split(sentences, target = 200)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 200 })
        assertTrue(parts.dropLast(1).all { it.endsWith(".") })
        assertEquals(squash(sentences), squash(parts.joinToString("")))
    }

    @Test
    fun aSingleLongWordIsCutHardAsALastResort() {
        val text = "a".repeat(1000)
        val parts = MaterialSplitter.split(text, target = 300)
        assertTrue(parts.all { it.length <= 300 })
        assertEquals(text, parts.joinToString(""))
    }

    @Test
    fun wordsAreNeverCutWhenASpaceIsAvailable() {
        val text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda"
        val parts = MaterialSplitter.split(text, target = 20)
        val words = text.split(" ").toSet()
        assertTrue(parts.flatMap { it.split(" ") }.all { it in words })
    }

    @Test
    fun sectionsAreTrimmedAndNeverBlank() {
        val parts = MaterialSplitter.split("one\n\n\n\n   \n\ntwo\n\n", target = 5)
        assertEquals(listOf("one", "two"), parts)
    }

    @Test
    fun smallParagraphsAreGroupedUpToTheTarget() {
        val parts = MaterialSplitter.split("a\n\nb\n\nc\n\nd", target = 100)
        assertEquals(1, parts.size)
    }
}

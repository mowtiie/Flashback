package com.mowtiie.flashback.ui.study

import com.mowtiie.flashback.ui.study.ChoiceBuilder.Choices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class ChoiceBuilderTest {

    private fun build(correct: String, vararg candidates: String): Choices? =
        ChoiceBuilder.build(correct, candidates.toList(), Random(7))

    @Test
    fun includesCorrectAnswerAtReportedIndex() {
        val choices = build("house", "dog", "forest", "cat", "tree")
        assertNotNull(choices)
        assertEquals("house", choices!!.options[choices.correctIndex])
    }

    @Test
    fun capsOptionsAtFour() {
        val choices = build("house", "dog", "forest", "cat", "tree", "sun")
        assertEquals(ChoiceBuilder.MAX_OPTIONS, choices!!.options.size)
    }

    @Test
    fun usesFewerOptionsWhenPoolIsSmall() {
        val choices = build("house", "dog", "cat")
        assertEquals(3, choices!!.options.size)
    }

    @Test
    fun optionsAreUnique() {
        val choices = build("house", "dog", "Dog", " dog ", "cat", "cat.")
        assertEquals(3, choices!!.options.size)
        assertEquals(3, choices.options.toSet().size)
    }

    @Test
    fun candidateMatchingTheAnswerIsNotADistractor() {
        val choices = build("house", "House", "dog")
        assertEquals(2, choices!!.options.size)
    }

    @Test
    fun noDistractorsGivesNull() {
        assertNull(build("house"))
        assertNull(build("house", "house", "HOUSE "))
    }

    @Test
    fun blankAnswerGivesNull() {
        assertNull(build("  ", "dog", "cat"))
        assertNull(ChoiceBuilder.build(null, listOf("dog"), Random(1)))
    }

    @Test
    fun overlongAnswerGivesNull() {
        val longText = "a".repeat(ChoiceBuilder.MAX_LENGTH + 1)
        assertNull(build(longText, "dog", "cat"))
    }

    @Test
    fun overlongCandidatesAreSkipped() {
        val longText = "a".repeat(ChoiceBuilder.MAX_LENGTH + 1)
        val choices = build("house", longText, "dog")
        assertEquals(2, choices!!.options.size)
        assertTrue(!choices.options.contains(longText))
    }

    @Test
    fun sameSeedGivesSameOrder() {
        val a = build("house", "dog", "cat", "tree")
        val b = build("house", "dog", "cat", "tree")
        assertEquals(a!!.options, b!!.options)
    }
}

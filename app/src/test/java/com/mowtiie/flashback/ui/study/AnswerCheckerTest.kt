package com.mowtiie.flashback.ui.study

import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.ui.study.AnswerChecker.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerCheckerTest {

    @Test
    fun exactMatchIsCorrect() {
        assertEquals(Result.CORRECT, AnswerChecker.check("the forest", "the forest"))
    }

    @Test
    fun caseAndWhitespaceAreIgnored() {
        assertEquals(Result.CORRECT, AnswerChecker.check("  The   FOREST ", "the forest"))
    }

    @Test
    fun accentsAreIgnored() {
        assertEquals(Result.CORRECT, AnswerChecker.check("cafe", "café"))
    }

    @Test
    fun voicedKanaIsNotTheSameAsItsUnvoicedForm() {
        assertEquals(Result.WRONG, AnswerChecker.check("か", "が"))
    }

    @Test
    fun hangulSyllablesAreComparedWholeNotAsJamo() {
        assertEquals(Result.CORRECT, AnswerChecker.check("한국어", "한국어"))
        assertEquals(Result.WRONG, AnswerChecker.check("한", "핸"))
    }

    @Test
    fun devanagariVowelSignsAreKept() {
        assertEquals(Result.WRONG, AnswerChecker.check("कि", "का"))
    }

    @Test
    fun trailingPunctuationIsIgnored() {
        assertEquals(Result.CORRECT, AnswerChecker.check("the forest", "The forest."))
    }

    @Test
    fun oneTypoInShortAnswerIsClose() {
        assertEquals(Result.CLOSE, AnswerChecker.check("forst", "forest"))
    }

    @Test
    fun twoTyposInLongAnswerAreClose() {
        assertEquals(Result.CLOSE, AnswerChecker.check("photosynthsis", "photosynthesis"))
        assertEquals(Result.CLOSE, AnswerChecker.check("photosynthessis", "photosynthesis"))
    }

    @Test
    fun tooManyTyposAreWrong() {
        assertEquals(Result.WRONG, AnswerChecker.check("forrrst", "forest"))
    }

    @Test
    fun veryShortAnswersNeedExactMatch() {
        assertEquals(Result.WRONG, AnswerChecker.check("cot", "cat"))
        assertEquals(Result.CORRECT, AnswerChecker.check("Cat", "cat"))
    }

    @Test
    fun blankInputIsWrong() {
        assertEquals(Result.WRONG, AnswerChecker.check("", "forest"))
        assertEquals(Result.WRONG, AnswerChecker.check("   ", "forest"))
        assertEquals(Result.WRONG, AnswerChecker.check(null, "forest"))
    }

    @Test
    fun blankExpectedIsWrong() {
        assertEquals(Result.WRONG, AnswerChecker.check("forest", ""))
    }

    @Test
    fun nonLatinTextMatchesExactly() {
        assertEquals(Result.CORRECT, AnswerChecker.check("森林", "森林"))
        assertEquals(Result.WRONG, AnswerChecker.check("木", "森林"))
    }

    @Test
    fun suggestedRatingFollowsResult() {
        assertEquals(Rating.GOOD, AnswerChecker.suggested(Result.CORRECT))
        assertEquals(Rating.HARD, AnswerChecker.suggested(Result.CLOSE))
        assertEquals(Rating.AGAIN, AnswerChecker.suggested(Result.WRONG))
    }
}

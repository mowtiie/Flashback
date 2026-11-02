package com.mowtiie.flashback.generator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationSessionTest {

    private val session = GenerationSession()

    @Test
    fun nothingIsRunningAtFirst() {
        assertNull(session.progress.value)
    }

    @Test
    fun beginStartsAtZeroWithTheTotal() {
        session.begin(total = 9) {}

        assertEquals(GenerationSession.Progress(finished = 0, total = 9, cards = 0), session.progress.value)
    }

    @Test
    fun updateKeepsTheTotal() {
        session.begin(total = 9) {}
        session.update(finished = 4, cards = 57)

        assertEquals(GenerationSession.Progress(finished = 4, total = 9, cards = 57), session.progress.value)
    }

    @Test
    fun updateWithoutARunIsIgnored() {
        session.update(finished = 1, cards = 1)

        assertNull(session.progress.value)
    }

    @Test
    fun endClearsTheProgressAndTheCancelAction() {
        var cancelled = 0
        session.begin(total = 2) { cancelled++ }
        session.end()
        session.cancel()

        assertNull(session.progress.value)
        assertEquals(0, cancelled)
    }

    @Test
    fun cancelRunsTheActionOfTheCurrentRun() {
        var first = 0
        var second = 0
        session.begin(total = 2) { first++ }
        session.begin(total = 3) { second++ }
        session.cancel()

        assertEquals(0, first)
        assertEquals(1, second)
        assertTrue(session.progress.value != null)
    }
}

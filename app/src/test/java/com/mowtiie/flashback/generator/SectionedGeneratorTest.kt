package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectionedGeneratorTest {

    private fun deck(vararg fronts: String, name: String = "Deck") =
        DeckArchive.Deck(name = name, notes = fronts.map { DeckArchive.Note(it, "answer of $it") })

    private val sections = listOf("first part", "second part", "third part")
    private val pauses = mutableListOf<Long>()
    private val calls = mutableListOf<GeneratorRequest>()

    private fun textOf(request: GeneratorRequest) = (request.input as GeneratorInput.Text).text

    private fun runner(script: suspend (GeneratorRequest) -> DeckArchive.Deck) =
        SectionedGenerator(
            generator = { request ->
                calls += request
                script(request)
            },
            pause = { pauses += it },
        )

    private suspend fun SectionedGenerator.runAll(
        outcomes: MutableList<SectionOutcome> = mutableListOf(),
        indices: List<Int> = sections.indices.toList(),
        known: () -> List<String> = { emptyList() },
    ): List<SectionOutcome> {
        run(sections, indices, CardTypes.Setting, known) { outcomes += it }
        return outcomes
    }

    @Test
    fun everySectionIsSentOnceInOrderWithTheEverythingCoverage() = runTest {
        val outcomes = runner { deck("q-${textOf(it)}") }.runAll()

        assertEquals(listOf("first part", "second part", "third part"), calls.map(::textOf))
        assertTrue(calls.all { it.coverage == Coverage.Everything })
        assertEquals(listOf(0, 1, 2), outcomes.map { it.index })
        assertTrue(outcomes.all { it is SectionOutcome.Done })
    }

    @Test
    fun aFailedSectionKeepsTheOthers() = runTest {
        val outcomes = runner {
            if (textOf(it) == "second part") throw GeneratorException("It broke.") else deck("q-${textOf(it)}")
        }.runAll()

        assertTrue(outcomes[0] is SectionOutcome.Done)
        assertEquals("It broke.", (outcomes[1] as SectionOutcome.Failed).message)
        assertTrue(outcomes[2] is SectionOutcome.Done)
    }

    @Test
    fun retryingSendsOnlyTheChosenSections() = runTest {
        val outcomes = runner { deck("q-${textOf(it)}") }.runAll(indices = listOf(1))

        assertEquals(listOf("second part"), calls.map(::textOf))
        assertEquals(listOf(1), outcomes.map { it.index })
    }

    @Test
    fun laterSectionsCarryTheFrontsAlreadyMade() = runTest {
        val made = mutableListOf<String>()
        runner { deck("q-${textOf(it)}") }.run(sections, sections.indices.toList(), CardTypes.Setting, { made.toList() }) {
            if (it is SectionOutcome.Done) made += it.deck.notes.map { n -> n.front }
        }

        assertEquals(emptyList<String>(), calls[0].known)
        assertEquals(listOf("q-first part"), calls[1].known)
        assertEquals(listOf("q-first part", "q-second part"), calls[2].known)
    }

    @Test
    fun aRejectedKeyStopsAtOnceAndMarksTheRestUnsent() = runTest {
        val outcomes = runner {
            throw GeneratorException("Anthropic rejected the API key.", kind = FailureKind.BadKey)
        }.runAll()

        assertEquals(1, calls.size)
        assertEquals(3, outcomes.size)
        assertTrue(outcomes.all { it is SectionOutcome.Failed })
        assertTrue((outcomes[2] as SectionOutcome.Failed).message.contains("key", ignoreCase = true))
    }

    @Test
    fun aRateLimitWaitsThenRetriesAndSucceeds() = runTest {
        var attempts = 0
        val outcomes = runner {
            if (textOf(it) == "first part" && attempts++ < 2) {
                throw GeneratorException("Slow down.", kind = FailureKind.RateLimited)
            }
            deck("q-${textOf(it)}")
        }.runAll()

        assertTrue(outcomes.all { it is SectionOutcome.Done })
        assertEquals(2, pauses.size)
        assertTrue(pauses.all { it > 0 })
    }

    @Test
    fun aRateLimitThatNeverClearsFailsTheSectionAfterAFewTries() = runTest {
        val outcomes = runner {
            if (textOf(it) == "first part") throw GeneratorException("Slow down.", kind = FailureKind.RateLimited)
            deck("q-${textOf(it)}")
        }.runAll()

        assertEquals(1 + SectionedGenerator.RATE_LIMIT_RETRIES, calls.count { textOf(it) == "first part" })
        assertTrue(outcomes[0] is SectionOutcome.Failed)
        assertTrue(outcomes[1] is SectionOutcome.Done)
    }

    @Test
    fun repeatedFailuresStopTheRunSoTheKeyIsNotBurned() = runTest {
        val many = List(10) { "part $it" }
        val outcomes = mutableListOf<SectionOutcome>()
        runner { throw GeneratorException("Down.") }
            .run(many, many.indices.toList(), CardTypes.Setting, { emptyList() }) { outcomes += it }

        assertEquals(SectionedGenerator.MAX_CONSECUTIVE_FAILURES, calls.size)
        assertEquals(10, outcomes.size)
        assertTrue(outcomes.all { it is SectionOutcome.Failed })
    }

    @Test
    fun aSuccessResetsTheFailureStreak() = runTest {
        val many = List(8) { "part $it" }
        val outcomes = mutableListOf<SectionOutcome>()
        runner {
            if (textOf(it).last().digitToInt() % 2 == 0) throw GeneratorException("Down.") else deck("q-${textOf(it)}")
        }.run(many, many.indices.toList(), CardTypes.Setting, { emptyList() }) { outcomes += it }

        assertEquals(8, calls.size)
    }

    @Test
    fun cancellingKeepsFinishedSectionsAndSendsNothingMore() = runTest {
        val outcomes = mutableListOf<SectionOutcome>()
        val job = launch {
            runner {
                if (textOf(it) == "second part") awaitCancellation()
                deck("q-${textOf(it)}")
            }.runAll(outcomes)
        }
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertEquals(listOf(0), outcomes.map { it.index })
        // The third section was never sent.
        assertEquals(listOf("first part", "second part"), calls.map(::textOf))
    }
}

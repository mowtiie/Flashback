package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.delay

sealed interface SectionOutcome {
    val index: Int

    class Done(override val index: Int, val deck: DeckArchive.Deck) : SectionOutcome

    class Failed(
        override val index: Int,
        val message: String,
        val kind: FailureKind = FailureKind.Other,
    ) : SectionOutcome
}

/**
 * Sends the sections of a long material one at a time, so a failure costs one section and a bad key
 * or an outage cannot spend the user's money on every remaining request.
 */
class SectionedGenerator(
    private val generator: CardGenerator,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    /** [onOutcome] runs after each section, so cancelling keeps what was already made. */
    suspend fun run(
        sections: List<String>,
        indices: List<Int>,
        types: CardTypes,
        known: () -> List<String>,
        coverage: Coverage = Coverage.Everything,
        cardLimit: Int? = null,
        onOutcome: (SectionOutcome) -> Unit,
    ) {
        var failures = 0
        for ((position, index) in indices.withIndex()) {
            val outcome = send(sections[index], index, types, known(), coverage, cardLimit)
            onOutcome(outcome)
            if (outcome is SectionOutcome.Done) {
                failures = 0
                continue
            }
            failures++
            val badKey = (outcome as SectionOutcome.Failed).kind == FailureKind.BadKey
            if (badKey || failures >= MAX_CONSECUTIVE_FAILURES) {
                val reason = if (badKey) NOT_SENT_KEY else NOT_SENT_ERRORS
                indices.drop(position + 1).forEach { onOutcome(SectionOutcome.Failed(it, reason)) }
                return
            }
        }
    }

    private suspend fun send(
        text: String,
        index: Int,
        types: CardTypes,
        known: List<String>,
        coverage: Coverage,
        cardLimit: Int?,
    ): SectionOutcome {
        val request = GeneratorRequest(
            input = GeneratorInput.Text(text),
            cardTypes = types,
            coverage = coverage,
            known = known,
            cardLimit = cardLimit,
        )
        var waits = 0
        while (true) {
            try {
                return SectionOutcome.Done(index, generator.generate(request))
            } catch (e: GeneratorException) {
                // After earlier cards, an empty reply means the part had nothing new. That is not a failure,
                // so it does not count toward stopping the run.
                if (e.kind == FailureKind.NoCards && known.isNotEmpty()) {
                    return SectionOutcome.Done(index, DeckArchive.Deck(name = "", notes = emptyList()))
                }
                if (e.kind == FailureKind.RateLimited && waits < RATE_LIMIT_RETRIES) {
                    waits++
                    pause(RATE_LIMIT_WAIT_MS * waits)
                    continue
                }
                return SectionOutcome.Failed(index, e.message.orEmpty(), e.kind)
            }
        }
    }

    companion object {
        const val RATE_LIMIT_RETRIES = 2
        const val MAX_CONSECUTIVE_FAILURES = 3
        const val RATE_LIMIT_WAIT_MS = 20_000L
        const val NOT_SENT_KEY = "Not started, because your API key was refused."
        const val NOT_SENT_ERRORS = "Not started, because the earlier parts kept failing."
    }
}

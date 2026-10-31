package com.mowtiie.flashback.ui.generator

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.generator.CardCount
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.CardTypes
import com.mowtiie.flashback.generator.Coverage
import com.mowtiie.flashback.generator.CoverageMerger
import com.mowtiie.flashback.generator.GeneratorException
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.GeneratorLimits
import com.mowtiie.flashback.generator.GeneratorPlan
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.generator.GeneratorRequest
import com.mowtiie.flashback.generator.MaterialSplitter
import com.mowtiie.flashback.generator.SectionOutcome
import com.mowtiie.flashback.generator.SectionedGenerator
import com.mowtiie.flashback.repository.ArchiveDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GeneratorStage { Input, Generating, Review, Saving }

/** A section that made no cards. [number] counts from 1, as the user sees it. */
@Immutable
data class FailedSection(val number: Int, val message: String)

@Immutable
data class GeneratorUiState(
    val hasApiKey: Boolean = false,
    val provider: GeneratorProvider = GeneratorProvider.Anthropic,
    val stage: GeneratorStage = GeneratorStage.Input,
    val source: String = "",
    val pdfName: String? = null,
    val pdfSize: Int = 0,
    val coverage: Coverage = Coverage.Everything,
    /** Only used by key points. */
    val cardCount: CardCount = CardCount.Normal,
    /** Null until the user picks; the screen has no default so the choice is deliberate. */
    val cardTypes: CardTypes? = null,
    /** Shown on the input page after a failed attempt. */
    val error: String? = null,
    val deckName: String = "",
    val cards: List<DeckArchive.Note> = emptyList(),
    /** Sections in the run and how many have finished or failed, for the progress line. */
    val sectionTotal: Int = 0,
    val sectionsFinished: Int = 0,
    /** Cards made from each section, in order. */
    val sectionCounts: List<Int> = emptyList(),
    val failedSections: List<FailedSection> = emptyList(),
    /** A run of many requests waits for a yes before anything is sent. */
    val confirming: Boolean = false,
) {
    val sourceTooLong: Boolean get() = source.length > GeneratorPlan.maxChars(coverage)

    val hasPdf: Boolean get() = pdfName != null

    /** A rough count from the length, shown before the real split is known. */
    val estimatedRequests: Int get() = GeneratorPlan.requests(coverage, source.trim().length, hasPdf)

    val canGenerate: Boolean
        get() = hasApiKey && stage == GeneratorStage.Input && !confirming && cardTypes != null &&
            (hasPdf || (source.isNotBlank() && !sourceTooLong))

    val canSave: Boolean
        get() = stage == GeneratorStage.Review && deckName.isNotBlank() && cards.isNotEmpty()
}

sealed interface GeneratorEvent {
    data object Saved : GeneratorEvent

    data object SaveFailed : GeneratorEvent
}

/** Generate, review, then save. Nothing is written until the user saves the reviewed cards. */
class GeneratorViewModel(
    private val generator: CardGenerator,
    private val archive: ArchiveDataSource,
    private val hasApiKey: () -> Boolean,
    private val provider: () -> GeneratorProvider,
    private val ioDispatcher: CoroutineDispatcher,
    pause: suspend (Long) -> Unit = { delay(it) },
) : ViewModel() {

    private val sectioned = SectionedGenerator(generator, pause)

    private val _uiState = MutableStateFlow(GeneratorUiState(hasApiKey = hasApiKey(), provider = provider()))
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private val events = Channel<GeneratorEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    /** The provider and key are set in Settings, so the screen re-checks when it comes back. */
    fun refreshKey() {
        _uiState.update { it.copy(hasApiKey = hasApiKey(), provider = provider()) }
    }

    fun setSource(text: String) {
        _uiState.update { it.copy(source = text, error = null) }
    }

    /** Held here, not in the state, since it can be megabytes. [reader] is blocking and runs off the main thread. */
    private var pdf: GeneratorInput.Pdf? = null

    fun attachPdf(reader: () -> GeneratorInput.Pdf) {
        viewModelScope.launch {
            try {
                val picked = withContext(ioDispatcher) { reader() }
                pdf = picked
                _uiState.update { it.copy(pdfName = picked.name, pdfSize = picked.bytes.size, error = null) }
            } catch (e: GeneratorException) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removePdf() {
        pdf = null
        _uiState.update { it.copy(pdfName = null, pdfSize = 0, error = null) }
    }

    fun setCardTypes(types: CardTypes) {
        _uiState.update { it.copy(cardTypes = types) }
    }

    fun setCardCount(count: CardCount) {
        _uiState.update { it.copy(cardCount = count) }
    }

    fun setCoverage(coverage: Coverage) {
        _uiState.update { it.copy(coverage = coverage, error = null) }
    }

    // The run: the sections sent, what each one made, and which cards the user removed since.
    private var sections: List<String> = emptyList()
    private val results = sortedMapOf<Int, DeckArchive.Deck>()
    private val failures = sortedMapOf<Int, String>()
    private val removed = mutableSetOf<String>()
    private var job: Job? = null

    fun generate() {
        val state = _uiState.value
        if (!state.canGenerate) return
        val types = state.cardTypes ?: return
        clearRun()
        _uiState.update { it.copy(stage = GeneratorStage.Generating, error = null, sectionTotal = 0, sectionsFinished = 0) }
        job = viewModelScope.launch {
            val picked = pdf
            if (picked != null || state.coverage == Coverage.KeyPoints) {
                val input = picked ?: GeneratorInput.Text(state.source.trim())
                single(GeneratorRequest(input, state.cardCount, types, state.coverage))
                return@launch
            }
            sections = withContext(ioDispatcher) { MaterialSplitter.split(state.source, GeneratorLimits.SECTION_CHARS) }
            if (GeneratorPlan.needsConfirmation(sections.size)) {
                _uiState.update { it.copy(stage = GeneratorStage.Input, confirming = true, sectionTotal = sections.size) }
            } else {
                runSections(sections.indices.toList(), types)
            }
        }
    }

    /** The user said yes to a run of many requests. */
    fun confirmGenerate() {
        val types = _uiState.value.cardTypes ?: return
        if (!_uiState.value.confirming) return
        _uiState.update { it.copy(confirming = false, stage = GeneratorStage.Generating, sectionsFinished = 0) }
        job = viewModelScope.launch { runSections(sections.indices.toList(), types) }
    }

    fun dismissConfirm() {
        sections = emptyList()
        _uiState.update { it.copy(confirming = false, sectionTotal = 0) }
    }

    /** Sends only the sections that made nothing, with the cards so far as "already made". */
    fun retryFailed() {
        val state = _uiState.value
        val types = state.cardTypes ?: return
        if (state.stage != GeneratorStage.Review || state.failedSections.isEmpty()) return
        val indices = failures.keys.toList()
        _uiState.update { it.copy(stage = GeneratorStage.Generating, error = null) }
        job = viewModelScope.launch { runSections(indices, types) }
    }

    /** Stops sending. Finished sections stay; the rest can be retried. */
    fun cancelGeneration() {
        if (_uiState.value.stage != GeneratorStage.Generating) return
        job?.cancel()
        sections.indices.filter { it !in results && it !in failures }.forEach { failures[it] = STOPPED }
        publish()
        finish()
    }

    private suspend fun single(request: GeneratorRequest) {
        sections = listOf("")
        _uiState.update { it.copy(sectionTotal = 1) }
        try {
            results[0] = generator.generate(request)
            publish()
            finish()
        } catch (e: GeneratorException) {
            _uiState.update { it.copy(stage = GeneratorStage.Input, error = e.message) }
        }
    }

    private suspend fun runSections(indices: List<Int>, types: CardTypes) {
        _uiState.update { it.copy(sectionTotal = sections.size) }
        sectioned.run(sections, indices, types, known = { merged().notes.map { n -> n.front } }) { outcome ->
            when (outcome) {
                is SectionOutcome.Done -> {
                    results[outcome.index] = outcome.deck
                    failures.remove(outcome.index)
                }
                is SectionOutcome.Failed -> failures[outcome.index] = outcome.message
            }
            publish()
        }
        finish()
    }

    private fun merged() = CoverageMerger.merge(sections.indices.map { results[it] })

    private fun publish() {
        val merged = merged()
        val notes = merged.notes.filter { it.front.trim().lowercase() !in removed }
        _uiState.update {
            it.copy(
                cards = notes,
                deckName = it.deckName.ifBlank { merged.name },
                sectionCounts = merged.perSection,
                sectionsFinished = results.size + failures.size,
                failedSections = failures.map { (index, message) -> FailedSection(index + 1, message) },
            )
        }
    }

    /** With nothing made, go back to the input page with the first reason; otherwise review what exists. */
    private fun finish() {
        if (results.isEmpty()) {
            val reason = failures.values.firstOrNull()?.takeUnless { it == STOPPED }
            _uiState.update {
                it.copy(
                    stage = GeneratorStage.Input,
                    error = reason,
                    cards = emptyList(),
                    failedSections = emptyList(),
                    deckName = "",
                )
            }
        } else {
            _uiState.update { it.copy(stage = GeneratorStage.Review) }
        }
    }

    private fun clearRun() {
        sections = emptyList()
        results.clear()
        failures.clear()
        removed.clear()
    }

    fun setDeckName(name: String) {
        _uiState.update { it.copy(deckName = name.take(MAX_DECK_NAME)) }
    }

    fun removeCard(index: Int) {
        val front = _uiState.value.cards.getOrNull(index)?.front ?: return
        removed += front.trim().lowercase()
        publish()
    }

    /** Back to the input page with the text kept, to adjust it and generate again. */
    fun startOver() {
        if (_uiState.value.stage != GeneratorStage.Review) return
        clearRun()
        _uiState.update {
            it.copy(
                stage = GeneratorStage.Input,
                cards = emptyList(),
                deckName = "",
                failedSections = emptyList(),
                sectionCounts = emptyList(),
                sectionTotal = 0,
                sectionsFinished = 0,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(stage = GeneratorStage.Saving) }
        viewModelScope.launch {
            val deck = DeckArchive.Deck(name = state.deckName.trim(), notes = state.cards)
            // The user has confirmed, so leaving the screen must not roll the save back.
            val result = withContext(NonCancellable) { archive.importArchive(DeckArchive(decks = listOf(deck))) }
            if (result == null) {
                _uiState.update { it.copy(stage = GeneratorStage.Review) }
                events.send(GeneratorEvent.SaveFailed)
            } else {
                events.send(GeneratorEvent.Saved)
            }
        }
    }

    private companion object {
        const val MAX_DECK_NAME = 200
        const val STOPPED = "Stopped before this section was sent."
    }
}

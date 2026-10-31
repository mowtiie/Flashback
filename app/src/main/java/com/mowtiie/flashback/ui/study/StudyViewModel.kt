package com.mowtiie.flashback.ui.study

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.model.StudyCard
import com.mowtiie.flashback.repository.StudyDataSource
import com.mowtiie.flashback.scheduler.CardState
import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.scheduler.SchedulingState
import com.mowtiie.flashback.scheduler.Sm2Scheduler
import com.mowtiie.flashback.util.Interval
import com.mowtiie.flashback.util.IntervalFormatter
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

@Immutable
data class StudyCounts(val newCards: Int = 0, val learning: Int = 0, val due: Int = 0)

@Immutable
data class StudyCardUi(val question: String, val answer: String)

sealed interface StudyEnding {
    data object NothingDue : StudyEnding

    data class Finished(val answered: Int) : StudyEnding

    /** Only learning cards are left and none is close enough to pull forward. */
    data class Waiting(val minutes: Long) : StudyEnding
}

@Immutable
data class StudyUiState(
    val answerMode: AnswerMode = AnswerMode.Flip,
    val loading: Boolean = true,
    val deckName: String? = null,
    val card: StudyCardUi? = null,
    val revealed: Boolean = false,
    val typed: String = "",
    val checkResult: AnswerChecker.Result? = null,
    val choices: ChoiceBuilder.Choices? = null,
    val selectedChoice: Int? = null,
    val counts: StudyCounts = StudyCounts(),
    val canUndo: Boolean = false,
    val previews: Map<Rating, Interval> = emptyMap(),
    val ending: StudyEnding? = null,
) {
    /** A choice card with too few distractors falls back to flip. */
    val showChoices: Boolean get() = answerMode == AnswerMode.Choice && choices != null

    val suggestedRating: Rating? get() = checkResult?.let(AnswerChecker::suggested)
}

sealed interface StudyEvent {
    data object Undone : StudyEvent

    data object CardSuspended : StudyEvent

    class EditCard(val noteId: Long) : StudyEvent
}

class StudyViewModel(
    private val data: StudyDataSource,
    private val deckId: Long,
    private val answerMode: AnswerMode,
    private val clock: () -> Long = System::currentTimeMillis,
    private val random: Random = Random(),
) : ViewModel() {

    private class Answered(val card: StudyCard, val before: SchedulingState)

    private val queue = StudyQueue()
    private val history = ArrayDeque<Answered>()

    /** Button previews only; separate because Sm2Scheduler is not thread safe. */
    private val previewScheduler = Sm2Scheduler()

    private var current: StudyCard? = null
    private var deckName: String? = null
    private var loading = true
    private var revealed = false
    private var typed = ""
    private var checkResult: AnswerChecker.Result? = null
    private var choices: ChoiceBuilder.Choices? = null
    private var selectedChoice: Int? = null
    private var previews: Map<Rating, Interval> = emptyMap()
    private var ending: StudyEnding? = null
    private var pool: List<Note> = emptyList()
    private var lastSuspended: StudyCard? = null
    private var cardShownAt = 0L
    private var answeredThisSession = 0
    private var busy = false
    private var awaitingEdit = false

    private val _uiState = MutableStateFlow(StudyUiState(answerMode = answerMode))
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val events = Channel<StudyEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            deckName = data.getDeck(deckId)?.name
            emit()
            if (answerMode == AnswerMode.Choice) {
                pool = data.getDeckNotes(deckId)
            }
            loadSession()
        }
    }

    private suspend fun loadSession() {
        queue.seed(data.buildStudyQueue(deckId, clock()))
        loading = false
        if (queue.isEmpty()) {
            ending = StudyEnding.NothingDue
            emit()
        } else {
            advance()
        }
    }

    fun reveal() {
        val card = current ?: return
        if (revealed) return
        revealed = true
        publishPreviews(card)
        emit()
    }

    fun onTypedChange(text: String) {
        typed = text
        emit()
    }

    fun check() {
        val card = current ?: return
        if (revealed) return
        checkResult = AnswerChecker.check(typed, card.answer())
        reveal()
    }

    fun choose(index: Int) {
        val options = choices ?: return
        if (current == null || revealed) return
        selectedChoice = index
        checkResult = if (index == options.correctIndex) {
            AnswerChecker.Result.CORRECT
        } else {
            AnswerChecker.Result.WRONG
        }
        reveal()
    }

    fun answer(rating: Rating) {
        val card = current ?: return
        if (busy || !revealed) return
        busy = true

        viewModelScope.launch {
            val now = clock()
            val before = card.card.toSchedulingState()
            val result = durable {
                data.answerCard(card.card.id, rating, now - cardShownAt, now)
            }
            busy = false
            if (result == null) {
                advance()
                return@launch
            }
            card.card.applySchedulingState(result.after)
            queue.requeue(card)

            history.addLast(Answered(card, before))
            answeredThisSession++
            advance()
        }
    }

    fun undo() {
        if (history.isEmpty() || busy) return
        busy = true

        viewModelScope.launch {
            val cardId = durable { data.undoLastReview() }
            busy = false
            if (cardId < 0) return@launch

            val entry = history.removeLast()
            queue.remove(entry.card)
            entry.card.card.applySchedulingState(entry.before)

            // The undone card may itself be on screen already, pulled back in by learn-ahead.
            current?.takeIf { it !== entry.card }?.let(queue::restore)

            answeredThisSession = maxOf(0, answeredThisSession - 1)
            show(entry.card)
            events.send(StudyEvent.Undone)
        }
    }

    fun suspendCurrent() {
        val card = current ?: return
        if (busy) return
        queue.removeNote(card.card.noteId)
        lastSuspended = card
        events.trySend(StudyEvent.CardSuspended)
        viewModelScope.launch { durable { data.setNoteSuspended(card.card.noteId, true) } }
        advance()
    }

    fun undoSuspend() {
        val card = lastSuspended ?: return
        lastSuspended = null
        viewModelScope.launch { durable { data.setNoteSuspended(card.card.noteId, false) } }

        current?.let(queue::restore)
        show(card)
    }

    fun editCard() {
        val card = current ?: return
        awaitingEdit = true
        events.trySend(StudyEvent.EditCard(card.card.noteId))
    }

    /** Call on every resume; it only does something when the card was just sent off to be edited. */
    fun onResumed() {
        if (!awaitingEdit) return
        awaitingEdit = false
        refreshCurrent()
    }

    private fun refreshCurrent() {
        val card = current ?: return
        viewModelScope.launch {
            val note = data.getNote(card.card.noteId)
            if (current !== card) return@launch
            if (note == null) {
                queue.removeNote(card.card.noteId)
                advance()
                return@launch
            }
            card.front = note.front
            card.back = note.back
            if (!revealed) choices = buildChoices(card)
            emit()
        }
    }

    private fun advance() {
        val next = queue.next(clock())
        if (next != null) {
            show(next)
            return
        }

        current = null
        revealed = false
        checkResult = null
        selectedChoice = null
        choices = null

        val dueAt = queue.nextLearningDueAt()
        ending = if (dueAt != null) {
            StudyEnding.Waiting(maxOf(1L, (dueAt - clock()) / MINUTE_MS))
        } else {
            StudyEnding.Finished(answeredThisSession)
        }
        emit()
    }

    private fun show(card: StudyCard) {
        cardShownAt = clock()
        revealed = false
        checkResult = null
        typed = ""
        selectedChoice = null
        choices = buildChoices(card)
        ending = null
        current = card
        emit()
    }

    private fun buildChoices(card: StudyCard): ChoiceBuilder.Choices? {
        if (answerMode != AnswerMode.Choice) return null
        val reverse = card.card.ordinal == Card.ORDINAL_REVERSE
        val candidates = pool
            .filter { it.id != card.card.noteId }
            .map { if (reverse) it.front else it.back }
        return ChoiceBuilder.build(card.answer(), candidates, random)
    }

    /** Previews use no fuzz, so the stored interval may differ slightly. */
    private fun publishPreviews(card: StudyCard) {
        val now = clock()
        previews = Rating.entries.associateWith { rating ->
            IntervalFormatter.format(
                previewScheduler.preview(card.card.toSchedulingState(), rating, now),
                now,
            )
        }
    }

    /** Counts include the card on screen, which the queue no longer holds. */
    private fun counts(): StudyCounts {
        var newCards = queue.countNew
        var learning = queue.countLearning
        var due = queue.countReview
        current?.let {
            when (it.card.state) {
                CardState.NEW -> newCards++
                CardState.REVIEW -> due++
                else -> learning++
            }
        }
        return StudyCounts(newCards, learning, due)
    }

    private fun emit() {
        val card = current
        _uiState.value = StudyUiState(
            answerMode = answerMode,
            loading = loading,
            deckName = deckName,
            card = card?.let { StudyCardUi(it.question(), it.answer()) },
            revealed = revealed,
            typed = typed,
            checkResult = checkResult,
            choices = choices,
            selectedChoice = selectedChoice,
            counts = counts(),
            canUndo = history.isNotEmpty(),
            previews = previews,
            ending = ending,
        )
    }

    /** A review must not be lost because the user left the screen while it was being written. */
    private suspend fun <T> durable(block: suspend () -> T): T =
        withContext(NonCancellable) { block() }

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}

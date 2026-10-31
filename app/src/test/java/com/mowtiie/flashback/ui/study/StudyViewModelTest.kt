package com.mowtiie.flashback.ui.study

import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.model.StudyCard
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.repository.StudyDataSource
import com.mowtiie.flashback.scheduler.CardState
import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.scheduler.SchedulerConfig
import com.mowtiie.flashback.scheduler.Sm2Scheduler
import com.mowtiie.flashback.util.Interval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Random
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {

    private class FakeData(
        var cards: List<StudyCard> = emptyList(),
        var notes: List<Note> = emptyList(),
    ) : StudyDataSource {
        val scheduler = Sm2Scheduler(
            SchedulerConfig().apply { fuzzEnabled = false },
            Random(1),
            TimeZone.getTimeZone("UTC"),
        )
        val suspendedCalls = mutableListOf<Pair<Long, Boolean>>()
        val answered = ArrayDeque<Pair<Card, com.mowtiie.flashback.scheduler.SchedulingState>>()
        var storedNote: Note? = null

        override suspend fun getDeck(deckId: Long) = Deck(id = deckId, name = "Spanish")

        override suspend fun getNote(noteId: Long) = storedNote

        override suspend fun getDeckNotes(deckId: Long) = notes

        override suspend fun buildStudyQueue(deckId: Long, now: Long) = cards

        override suspend fun answerCard(
            cardId: Long,
            rating: Rating,
            elapsedMs: Long,
            now: Long,
        ): FlashbackRepository.AnswerResult? {
            val card = cards.first { it.card.id == cardId }.card
            val before = card.toSchedulingState()
            val after = scheduler.answer(before, rating, now)
            answered.addLast(card to before)
            return FlashbackRepository.AnswerResult(answered.size.toLong(), after)
        }

        override suspend fun undoLastReview(): Long =
            answered.removeLastOrNull()?.first?.id ?: -1L

        override suspend fun setNoteSuspended(noteId: Long, suspended: Boolean) {
            suspendedCalls += noteId to suspended
        }
    }

    private var now = NOW

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun card(
        id: Long,
        noteId: Long = id,
        state: Int = CardState.NEW,
        dueAt: Long = 0,
        front: String = "front$id",
        back: String = "back$id",
        ordinal: Int = Card.ORDINAL_FORWARD,
    ) = StudyCard(Card(id = id, noteId = noteId, ordinal = ordinal, state = state, dueAt = dueAt), front, back)

    private fun viewModel(data: FakeData, mode: AnswerMode = AnswerMode.Flip) =
        StudyViewModel(data, deckId = 1, answerMode = mode, clock = { now }, random = Random(7))

    @Test
    fun emptyQueueEndsWithNothingDue() {
        val state = viewModel(FakeData()).uiState.value

        assertEquals(StudyEnding.NothingDue, state.ending)
        assertNull(state.card)
        assertFalse(state.loading)
        assertEquals("Spanish", state.deckName)
    }

    @Test
    fun firstCardIsShownHiddenAnswerAndCountsIncludeIt() {
        val vm = viewModel(FakeData(cards = listOf(card(1), card(2), card(3, state = CardState.REVIEW))))

        val state = vm.uiState.value

        assertEquals(StudyCardUi("front1", "back1"), state.card)
        assertFalse(state.revealed)
        assertEquals(StudyCounts(newCards = 2, learning = 0, due = 1), state.counts)
        assertFalse(state.canUndo)
        assertNull(state.ending)
    }

    @Test
    fun reverseCardAsksTheBackFirst() {
        val vm = viewModel(FakeData(cards = listOf(card(1, ordinal = Card.ORDINAL_REVERSE))))

        assertEquals(StudyCardUi("back1", "front1"), vm.uiState.value.card)
    }

    @Test
    fun revealingPublishesAnIntervalForEveryRating() {
        val vm = viewModel(FakeData(cards = listOf(card(1))))

        vm.reveal()

        val state = vm.uiState.value
        assertTrue(state.revealed)
        assertEquals(Rating.entries.toSet(), state.previews.keys)
        assertEquals(Interval.Minutes(1), state.previews[Rating.AGAIN])
        assertEquals(Interval.Minutes(10), state.previews[Rating.GOOD])
        assertEquals(Interval.Days(4), state.previews[Rating.EASY])
    }

    @Test
    fun answerIsIgnoredUntilTheAnswerIsRevealed() {
        val data = FakeData(cards = listOf(card(1), card(2)))
        val vm = viewModel(data)

        vm.answer(Rating.GOOD)

        assertTrue(data.answered.isEmpty())
        assertEquals("front1", vm.uiState.value.card?.question)
    }

    @Test
    fun answeringMovesOnAndLearningCardsComeBack() {
        val data = FakeData(cards = listOf(card(1), card(2)))
        val vm = viewModel(data)

        vm.reveal()
        vm.answer(Rating.GOOD)

        val state = vm.uiState.value
        assertEquals("front2", state.card?.question)
        assertFalse(state.revealed)
        assertTrue(state.canUndo)
        assertEquals(StudyCounts(newCards = 1, learning = 1, due = 0), state.counts)
    }

    @Test
    fun finishingEveryCardEndsTheSessionWithTheAnswerCount() {
        val vm = viewModel(FakeData(cards = listOf(card(1), card(2))))

        repeat(2) {
            vm.reveal()
            vm.answer(Rating.EASY)
        }

        assertEquals(StudyEnding.Finished(answered = 2), vm.uiState.value.ending)
        assertNull(vm.uiState.value.card)
    }

    @Test
    fun againThenTwoGoodsGraduatesTheCardOutOfTheSession() {
        val vm = viewModel(FakeData(cards = listOf(card(1))))

        vm.reveal()
        vm.answer(Rating.AGAIN) // due in 1 minute: inside the learn-ahead window, comes straight back
        vm.reveal()
        vm.answer(Rating.GOOD)
        vm.reveal()
        vm.answer(Rating.GOOD) // graduates out of learning

        assertEquals(StudyEnding.Finished(answered = 3), vm.uiState.value.ending)
    }

    @Test
    fun learningCardBeyondTheWindowShowsWaiting() {
        val vm = viewModel(FakeData(cards = listOf(card(1, state = CardState.LEARNING, dueAt = NOW + 45 * MIN))))

        assertEquals(StudyEnding.Waiting(minutes = 45), vm.uiState.value.ending)
    }

    @Test
    fun undoRestoresTheAnsweredCardAndPutsTheCurrentOneBack() = runTest {
        val data = FakeData(cards = listOf(card(1), card(2)))
        val vm = viewModel(data)
        vm.reveal()
        vm.answer(Rating.EASY)
        assertEquals("front2", vm.uiState.value.card?.question)

        vm.undo()

        val state = vm.uiState.value
        assertEquals("front1", state.card?.question)
        assertFalse(state.revealed)
        assertFalse(state.canUndo)
        assertEquals(StudyCounts(newCards = 2), state.counts)
        assertEquals(StudyEvent.Undone, vm.eventFlow.first())

        // The displaced card is still next in line.
        vm.reveal()
        vm.answer(Rating.EASY)
        assertEquals("front2", vm.uiState.value.card?.question)
    }

    @Test
    fun undoWithNothingAnsweredDoesNothing() {
        val vm = viewModel(FakeData(cards = listOf(card(1))))

        vm.undo()

        assertEquals("front1", vm.uiState.value.card?.question)
        assertFalse(vm.uiState.value.canUndo)
    }

    @Test
    fun undoReopensASessionThatHadFinished() = runTest {
        val vm = viewModel(FakeData(cards = listOf(card(1))))
        vm.reveal()
        vm.answer(Rating.EASY)
        assertTrue(vm.uiState.value.ending is StudyEnding.Finished)

        vm.undo()

        assertNull(vm.uiState.value.ending)
        assertEquals("front1", vm.uiState.value.card?.question)
    }

    @Test
    fun typedCorrectAnswerIsCheckedAndSuggestsGood() {
        val vm = viewModel(FakeData(cards = listOf(card(1, back = "the forest"))), AnswerMode.Type)

        vm.onTypedChange("The Forest.")
        vm.check()

        val state = vm.uiState.value
        assertTrue(state.revealed)
        assertEquals(AnswerChecker.Result.CORRECT, state.checkResult)
        assertEquals(Rating.GOOD, state.suggestedRating)
    }

    @Test
    fun typedWrongAnswerSuggestsAgainAndTypedTextIsKeptUntilTheNextCard() {
        val vm = viewModel(
            FakeData(cards = listOf(card(1, back = "the forest"), card(2))),
            AnswerMode.Type,
        )

        vm.onTypedChange("a house")
        vm.check()

        assertEquals(Rating.AGAIN, vm.uiState.value.suggestedRating)
        assertEquals("a house", vm.uiState.value.typed)

        vm.answer(Rating.AGAIN)
        vm.onTypedChange("x")
        assertEquals("x", vm.uiState.value.typed)
    }

    @Test
    fun typedTextResetsOnANewCard() {
        val vm = viewModel(FakeData(cards = listOf(card(1), card(2))), AnswerMode.Type)
        vm.onTypedChange("abc")
        vm.check()

        vm.answer(Rating.EASY)

        assertEquals("front2", vm.uiState.value.card?.question)
        assertEquals("", vm.uiState.value.typed)
        assertNull(vm.uiState.value.checkResult)
    }

    @Test
    fun choiceModeOffersTheAnswerAndDistractorsFromOtherNotes() {
        val data = FakeData(
            cards = listOf(card(1, back = "the forest")),
            notes = listOf(
                Note(id = 1, deckId = 1, front = "f1", back = "the forest"),
                Note(id = 2, deckId = 1, front = "f2", back = "the house"),
                Note(id = 3, deckId = 1, front = "f3", back = "the dog"),
            ),
        )
        val vm = viewModel(data, AnswerMode.Choice)

        val choices = vm.uiState.value.choices
        assertNotNull(choices)
        assertEquals(setOf("the forest", "the house", "the dog"), choices!!.options.toSet())
        assertTrue(vm.uiState.value.showChoices)
    }

    @Test
    fun choosingTheWrongOptionRevealsAndSuggestsAgain() {
        val data = FakeData(
            cards = listOf(card(1, back = "the forest")),
            notes = listOf(
                Note(id = 1, deckId = 1, front = "f1", back = "the forest"),
                Note(id = 2, deckId = 1, front = "f2", back = "the house"),
            ),
        )
        val vm = viewModel(data, AnswerMode.Choice)
        val choices = vm.uiState.value.choices!!
        val wrong = choices.options.indices.first { it != choices.correctIndex }

        vm.choose(wrong)

        val state = vm.uiState.value
        assertTrue(state.revealed)
        assertEquals(wrong, state.selectedChoice)
        assertEquals(Rating.AGAIN, state.suggestedRating)
    }

    @Test
    fun choosingTheRightOptionSuggestsGoodAndAFurtherChoiceIsIgnored() {
        val data = FakeData(
            cards = listOf(card(1, back = "the forest")),
            notes = listOf(
                Note(id = 1, deckId = 1, front = "f1", back = "the forest"),
                Note(id = 2, deckId = 1, front = "f2", back = "the house"),
            ),
        )
        val vm = viewModel(data, AnswerMode.Choice)
        val choices = vm.uiState.value.choices!!

        vm.choose(choices.correctIndex)
        vm.choose((choices.correctIndex + 1) % choices.options.size)

        assertEquals(choices.correctIndex, vm.uiState.value.selectedChoice)
        assertEquals(Rating.GOOD, vm.uiState.value.suggestedRating)
    }

    @Test
    fun choiceModeFallsBackToFlipWhenTheDeckHasNoOtherNotes() {
        val data = FakeData(
            cards = listOf(card(1, back = "the forest")),
            notes = listOf(Note(id = 1, deckId = 1, front = "f1", back = "the forest")),
        )
        val vm = viewModel(data, AnswerMode.Choice)

        assertNull(vm.uiState.value.choices)
        assertFalse(vm.uiState.value.showChoices)
    }

    @Test
    fun choiceDistractorsForAReverseCardComeFromTheFronts() {
        val data = FakeData(
            cards = listOf(card(1, front = "el bosque", back = "the forest", ordinal = Card.ORDINAL_REVERSE)),
            notes = listOf(
                Note(id = 1, deckId = 1, front = "el bosque", back = "the forest"),
                Note(id = 2, deckId = 1, front = "la casa", back = "the house"),
            ),
        )
        val vm = viewModel(data, AnswerMode.Choice)

        assertEquals(setOf("el bosque", "la casa"), vm.uiState.value.choices!!.options.toSet())
    }

    @Test
    fun suspendingDropsEveryCardOfThatNoteAndCanBeUndone() = runTest {
        val data = FakeData(
            cards = listOf(card(1, noteId = 10), card(2, noteId = 10, ordinal = 1), card(3, noteId = 11)),
        )
        val vm = viewModel(data)

        vm.suspendCurrent()

        assertEquals(listOf(10L to true), data.suspendedCalls)
        assertEquals("front3", vm.uiState.value.card?.question)
        assertEquals(StudyEvent.CardSuspended, vm.eventFlow.first())

        vm.undoSuspend()

        assertEquals(listOf(10L to true, 10L to false), data.suspendedCalls)
        assertEquals("front1", vm.uiState.value.card?.question)
        // Card 3 went back into the queue, not lost.
        vm.reveal()
        vm.answer(Rating.EASY)
        assertEquals("front3", vm.uiState.value.card?.question)
    }

    @Test
    fun refreshingPicksUpAnEditedCard() {
        val data = FakeData(cards = listOf(card(1)))
        val vm = viewModel(data)
        data.storedNote = Note(id = 1, deckId = 1, front = "edited front", back = "edited back")

        vm.editCard()
        vm.onResumed()

        assertEquals(StudyCardUi("edited front", "edited back"), vm.uiState.value.card)
    }

    @Test
    fun resumingWithoutHavingEditedLeavesTheCardAlone() {
        val data = FakeData(cards = listOf(card(1)))
        val vm = viewModel(data)
        data.storedNote = Note(id = 1, deckId = 1, front = "edited front", back = "edited back")

        vm.onResumed()

        assertEquals(StudyCardUi("front1", "back1"), vm.uiState.value.card)
    }

    @Test
    fun editingAsksTheScreenToOpenTheNote() = runTest {
        val vm = viewModel(FakeData(cards = listOf(card(1, noteId = 42))))

        vm.editCard()

        val event = vm.eventFlow.first() as StudyEvent.EditCard
        assertEquals(42L, event.noteId)
    }

    @Test
    fun refreshingSkipsACardWhoseNoteWasDeleted() {
        val data = FakeData(cards = listOf(card(1), card(2)))
        val vm = viewModel(data)
        data.storedNote = null

        vm.editCard()
        vm.onResumed()

        assertEquals("front2", vm.uiState.value.card?.question)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MIN = 60_000L
    }
}

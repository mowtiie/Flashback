package com.mowtiie.flashback.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.AppDispatchers
import com.mowtiie.flashback.data.FlashbackDatabase
import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.StudyCard
import com.mowtiie.flashback.scheduler.CardState
import com.mowtiie.flashback.scheduler.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/** In-memory Room with the real repository: per-deck daily limits, undo refund, tag-name conflicts. */
@RunWith(AndroidJUnit4::class)
class FlashbackRepositoryTest {

    private lateinit var db: FlashbackDatabase
    private lateinit var repository: FlashbackRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FlashbackDatabase::class.java).build()
        val dispatchers = AppDispatchers(
            write = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
            io = Dispatchers.IO,
        )
        repository = FlashbackRepository(db, dispatchers)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newDeck(newPerDay: Int, reviewsPerDay: Int): Long =
        repository.saveDeck(Deck(name = "Deck", newPerDay = newPerDay, reviewsPerDay = reviewsPerDay), emptyList())

    private suspend fun newNote(deckId: Long, front: String): Long =
        repository.addNote(Note(deckId = deckId, front = front, back = "back"))

    private suspend fun dueReviewCard(deckId: Long, front: String): Card {
        val card = db.cardDao().getByNote(newNote(deckId, front)).first()
        card.state = CardState.REVIEW
        card.intervalDays = 5
        card.dueAt = 1L
        db.cardDao().update(card)
        return card
    }

    private suspend fun queue(deckId: Long): List<StudyCard> =
        repository.buildStudyQueue(deckId, System.currentTimeMillis())

    private suspend fun answer(card: Card, rating: Rating) {
        val result = repository.answerCard(card.id, rating, 1000L, System.currentTimeMillis())
        assertNotNull(result)
    }

    @Test
    fun newCardLimitSurvivesReopeningTheSession() = runBlocking {
        val deckId = newDeck(2, 200)
        repeat(5) { newNote(deckId, "n$it") }

        val first = queue(deckId)
        assertEquals(2, first.size)
        first.forEach { answer(it.card, Rating.GOOD) }

        assertEquals(0, queue(deckId).size)
    }

    @Test
    fun undoRefundsTheNewCardSlot() = runBlocking {
        val deckId = newDeck(1, 200)
        newNote(deckId, "a")
        newNote(deckId, "b")

        val only = queue(deckId).first()
        answer(only.card, Rating.GOOD)
        assertEquals(0, queue(deckId).size)

        assertEquals(only.card.id, repository.undoLastReview())
        assertEquals(1, queue(deckId).size)
    }

    @Test
    fun undoRestoresTheSnapshotAndDropsTheLogRow() = runBlocking {
        val deckId = newDeck(20, 200)
        val before = db.cardDao().getByNote(newNote(deckId, "a")).first()

        answer(before, Rating.GOOD)
        assertEquals(CardState.LEARNING, db.cardDao().getById(before.id)!!.state)

        repository.undoLastReview()
        val restored = db.cardDao().getById(before.id)!!
        assertEquals(CardState.NEW, restored.state)
        assertEquals(0, restored.reps)
        assertEquals(0, db.reviewLogDao().totalReviews())
    }

    @Test
    fun undoWithNoHistoryDoesNothing() = runBlocking {
        assertEquals(-1L, repository.undoLastReview())
    }

    @Test
    fun reviewLimitIsPerDeck() = runBlocking {
        val deckA = newDeck(20, 1)
        val deckB = newDeck(20, 1)
        val a1 = dueReviewCard(deckA, "a1")
        dueReviewCard(deckA, "a2")
        dueReviewCard(deckB, "b1")

        answer(a1, Rating.GOOD)

        assertEquals("deck A has used its one review", 0, queue(deckA).size)
        assertEquals("deck B is unaffected", 1, queue(deckB).size)
    }

    @Test
    fun newCardAnswersDoNotUseTheReviewLimit() = runBlocking {
        val deckId = newDeck(20, 1)
        val newCard = db.cardDao().getByNote(newNote(deckId, "fresh")).first()
        dueReviewCard(deckId, "old")

        answer(newCard, Rating.GOOD)

        val remaining = queue(deckId)
        assertEquals(1, remaining.size)
        assertEquals(CardState.REVIEW, remaining.first().card.state)
    }

    @Test
    fun summaryCountsAndTodaysUsageMatchTheData() = runBlocking {
        val deckId = newDeck(3, 200)
        repeat(5) { newNote(deckId, "n$it") }
        val session = queue(deckId)
        assertEquals(3, session.size)
        answer(session[0].card, Rating.GOOD)
        answer(session[1].card, Rating.GOOD)

        val summary = repository.observeDeckSummary(deckId, System.currentTimeMillis()).first()!!
        assertEquals(3, summary.newCount)
        assertEquals(5, summary.totalCount)
        assertEquals(2, summary.newToday)
        assertEquals(0, summary.reviewsToday)
        assertEquals(1, summary.availableNew())
    }

    @Test
    fun summaryCountsReviewsPerDeckAndHandlesAnEmptyDeck() = runBlocking {
        val busy = newDeck(20, 200)
        val empty = newDeck(20, 200)
        val first = dueReviewCard(busy, "r1")
        dueReviewCard(busy, "r2")
        answer(first, Rating.GOOD)

        val now = System.currentTimeMillis()
        val busySummary = repository.observeDeckSummary(busy, now).first()!!
        assertEquals(1, busySummary.dueCount)
        assertEquals(1, busySummary.reviewsToday)

        val emptySummary = repository.observeDeckSummary(empty, now).first()!!
        assertEquals(0, emptySummary.totalCount)
        assertEquals(0, emptySummary.newToday)
        assertEquals(0, emptySummary.studyableCount())
    }

    @Test
    fun undoingANoteDeleteRestoresItsCardsAndHistory() = runBlocking {
        val deckId = newDeck(20, 200)
        val noteId = newNote(deckId, "keep my history")
        val card = db.cardDao().getByNote(noteId).first()
        answer(card, Rating.GOOD)
        assertEquals(1, db.reviewLogDao().getByCards(listOf(card.id)).size)

        val undo = repository.deleteNote(db.noteDao().getById(noteId)!!)
        assertEquals(0, db.cardDao().getByNote(noteId).size)

        assertTrue(undo.undo())
        assertEquals(1, db.cardDao().getByNote(noteId).size)
        assertEquals(CardState.LEARNING, db.cardDao().getById(card.id)!!.state)
        assertEquals(1, db.reviewLogDao().getByCards(listOf(card.id)).size)
    }

    @Test
    fun undoingADeckDeleteRestoresEverythingUnderIt() = runBlocking {
        val deckId = newDeck(20, 200)
        repeat(3) { newNote(deckId, "n$it") }
        val tagId = repository.saveTag(Tag(name = "Kept"))!!
        repository.saveDeck(db.deckDao().getById(deckId)!!, listOf(tagId))
        answer(db.cardDao().getByDeck(deckId).first(), Rating.GOOD)

        val undo = repository.deleteDeck(db.deckDao().getById(deckId)!!)
        assertNull(db.deckDao().getById(deckId))
        assertEquals(0, db.cardDao().getByDeck(deckId).size)

        assertTrue(undo.undo())
        assertEquals(3, db.noteDao().getByDeck(deckId).size)
        assertEquals(3, db.cardDao().getByDeck(deckId).size)
        assertEquals(1, db.reviewLogDao().getByDeck(deckId).size)
        assertEquals(listOf(tagId), db.tagDao().getTagIdsForDeck(deckId))
    }

    @Test
    fun undoingATagDeleteRelinksItsDecks() = runBlocking {
        val deckId = newDeck(20, 200)
        val tagId = repository.saveTag(Tag(name = "Relink"))!!
        repository.saveDeck(db.deckDao().getById(deckId)!!, listOf(tagId))

        val undo = repository.deleteTag(db.tagDao().getById(tagId)!!)
        assertEquals(0, db.tagDao().getTagIdsForDeck(deckId).size)

        assertTrue(undo.undo())
        assertEquals(listOf(tagId), db.tagDao().getTagIdsForDeck(deckId))
    }

    @Test
    fun undoingATagDeleteFailsWhenTheNameWasTakenMeanwhile() = runBlocking {
        val id = repository.saveTag(Tag(name = "Spanish"))!!
        val undo = repository.deleteTag(db.tagDao().getById(id)!!)
        val replacement = repository.saveTag(Tag(name = "Spanish"))!!

        assertFalse(undo.undo())
        assertEquals(listOf(replacement), db.tagDao().observeAll().first().map { it.id })
    }

    @Test
    fun savingADeckReplacesItsTags() = runBlocking {
        val deckId = newDeck(20, 200)
        val first = repository.saveTag(Tag(name = "First"))!!
        val second = repository.saveTag(Tag(name = "Second"))!!
        val deck = db.deckDao().getById(deckId)!!

        repository.saveDeck(deck, listOf(first))
        repository.saveDeck(deck, listOf(second))

        assertEquals(listOf(second), db.tagDao().getTagIdsForDeck(deckId))
    }

    @Test
    fun savingADeckSkipsATagDeletedWhileEditing() = runBlocking {
        val deckId = newDeck(20, 200)
        val kept = repository.saveTag(Tag(name = "Kept"))!!
        val gone = repository.saveTag(Tag(name = "Gone"))!!
        repository.deleteTag(db.tagDao().getById(gone)!!)

        repository.saveDeck(db.deckDao().getById(deckId)!!, listOf(kept, gone))

        assertEquals(listOf(kept), db.tagDao().getTagIdsForDeck(deckId))
    }

    @Test
    fun undoDoesNothingIfTheDeckIsGoneAgain() = runBlocking {
        val deckId = newDeck(20, 200)
        val noteId = newNote(deckId, "orphan")
        val undo = repository.deleteNote(db.noteDao().getById(noteId)!!)

        db.deckDao().delete(db.deckDao().getById(deckId)!!)

        assertFalse(undo.undo())
        assertEquals(0, db.noteDao().getByDeck(deckId).size)
    }

    @Test
    fun exportTextIsNullWhenThereIsNothingToExport() = runBlocking {
        assertNull(repository.exportAllText())
    }

    @Test
    fun exportTextContainsTheDeck() = runBlocking {
        val deckId = newDeck(20, 200)
        newNote(deckId, "serialize me")

        val all = repository.exportAllText()
        val one = repository.exportDeckText(deckId)
        assertTrue(all!!.contains("serialize me"))
        assertTrue(one!!.contains("serialize me"))
    }

    @Test
    fun suspendedNoteIsSkippedAndCanBeRestored() = runBlocking {
        val deckId = newDeck(20, 200)
        val noteId = newNote(deckId, "skip me")
        newNote(deckId, "keep me")
        assertEquals(2, queue(deckId).size)

        repository.setNoteSuspended(noteId, true)
        assertTrue(repository.isNoteSuspended(noteId))
        assertEquals(1, queue(deckId).size)

        repository.setNoteSuspended(noteId, false)
        assertFalse(repository.isNoteSuspended(noteId))
        assertEquals(2, queue(deckId).size)
    }

    @Test
    fun duplicateTagNameIsRefusedOnCreate() = runBlocking {
        assertNotNull(repository.saveTag(Tag(name = "Spanish")))
        assertNull(repository.saveTag(Tag(name = "Spanish")))
    }

    @Test
    fun renamingOntoAnExistingTagIsRefusedWithoutThrowing() = runBlocking {
        repository.saveTag(Tag(name = "One"))!!
        val secondId = repository.saveTag(Tag(name = "Two"))!!

        assertNull(repository.saveTag(Tag(id = secondId, name = "One")))
        assertEquals("Two", db.tagDao().getById(secondId)!!.name)
    }

    @Test
    fun resavingATagUnderItsOwnNameIsAllowed() = runBlocking {
        val id = repository.saveTag(Tag(name = "Keep"))!!

        assertEquals(id, repository.saveTag(Tag(id = id, name = "Keep", description = "edited")))
        assertEquals("edited", db.tagDao().getById(id)!!.description)
    }

    @Test
    fun updatingANoteToDropItsReverseDeletesThatCard() = runBlocking {
        val deckId = newDeck(20, 200)
        val noteId = repository.addNote(Note(deckId = deckId, front = "f", back = "b", reverseEnabled = true))
        assertEquals(2, db.cardDao().getByNote(noteId).size)

        repository.updateNote(db.noteDao().getById(noteId)!!.copy(reverseEnabled = false))
        assertEquals(listOf(Card.ORDINAL_FORWARD), db.cardDao().getByNote(noteId).map { it.ordinal })
    }
}

package com.mowtiie.flashback.repository

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.mowtiie.flashback.AppDispatchers
import com.mowtiie.flashback.data.FlashbackDatabase
import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.DeckTagCrossRef
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.entity.ReviewLog
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.data.model.DailyCount
import com.mowtiie.flashback.data.model.DeckSummary
import com.mowtiie.flashback.data.model.DeckWithTags
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.data.model.StudyCard
import com.mowtiie.flashback.data.model.TagWithCount
import com.mowtiie.flashback.scheduler.Rating
import com.mowtiie.flashback.scheduler.SchedulingState
import com.mowtiie.flashback.scheduler.Sm2Scheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

/** Single entry point to persistence for ViewModels. */
class FlashbackRepository(
    private val db: FlashbackDatabase,
    private val dispatchers: AppDispatchers,
    private val scheduler: Sm2Scheduler = Sm2Scheduler(),
) {
    private val deckDao = db.deckDao()
    private val noteDao = db.noteDao()
    private val cardDao = db.cardDao()
    private val tagDao = db.tagDao()
    private val reviewLogDao = db.reviewLogDao()

    class AnswerResult(
        val reviewLogId: Long,
        /** The card's state after the answer, so the session knows whether it comes back. */
        val after: SchedulingState,
    )

    fun interface UndoAction {
        /** False if something the restore needs is gone; nothing is written then. */
        suspend fun undo(): Boolean
    }

    fun observeDeckSummaries(now: Long): Flow<List<DeckSummary>> =
        deckDao.observeSummaries(now, startOfTodayMillis(now))

    fun observeDeck(deckId: Long): Flow<Deck?> = deckDao.observeById(deckId)

    fun observeDeckSummary(deckId: Long, now: Long): Flow<DeckSummary?> =
        deckDao.observeSummary(deckId, now, startOfTodayMillis(now))

    fun observeDecksWithTags(): Flow<List<DeckWithTags>> = deckDao.observeAllWithTags()

    fun observeNotes(deckId: Long): Flow<List<NotePreview>> = noteDao.observeByDeck(deckId)

    fun observeSuspendedNoteIds(deckId: Long): Flow<List<Long>> =
        cardDao.observeSuspendedNoteIds(deckId)

    fun observeAllTags(): Flow<List<Tag>> = tagDao.observeAll()

    fun observeTagsWithCounts(): Flow<List<TagWithCount>> = tagDao.observeAllWithCounts()

    fun observeTagsForDeck(deckId: Long): Flow<List<Tag>> = tagDao.observeTagsForDeck(deckId)

    suspend fun getDeck(deckId: Long): Deck? = write { deckDao.getById(deckId) }

    suspend fun getNote(noteId: Long): Note? = write { noteDao.getById(noteId) }

    suspend fun getTag(tagId: Long): Tag? = write { tagDao.getById(tagId) }

    suspend fun isNoteSuspended(noteId: Long): Boolean =
        write { cardDao.countSuspendedForNote(noteId) > 0 }

    suspend fun getDeckNotes(deckId: Long): List<Note> = io { noteDao.getByDeck(deckId) }

    suspend fun insertDeck(deck: Deck): Long = write { deckDao.insert(deck) }

    suspend fun updateDeck(deck: Deck) = write { deckDao.update(deck) }

    /** Inserts (id 0) or updates the deck and replaces its tags in one transaction. Returns the id. */
    suspend fun saveDeck(deck: Deck, tagIds: Collection<Long>): Long = write {
        db.withTransaction {
            val id = if (deck.id == 0L) {
                deckDao.insert(deck)
            } else {
                deckDao.update(deck)
                deck.id
            }
            tagDao.clearTagsForDeck(id)
            for (tagId in tagIds) {
                // A tag deleted while the editor was open is skipped rather than failing the save.
                if (tagDao.getById(tagId) != null) {
                    tagDao.link(DeckTagCrossRef(id, tagId))
                }
            }
            id
        }
    }

    suspend fun deleteDeck(deck: Deck): UndoAction {
        // Snapshot first: the cascade removes all of it with the deck.
        val snapshot = write {
            db.withTransaction {
                DeckSnapshot(
                    notes = noteDao.getByDeck(deck.id),
                    cards = cardDao.getByDeck(deck.id),
                    logs = reviewLogDao.getByDeck(deck.id),
                    tagIds = tagDao.getTagIdsForDeck(deck.id),
                ).also { deckDao.delete(deck) }
            }
        }
        return UndoAction {
            restore {
                deckDao.insert(deck)
                noteDao.insertAll(snapshot.notes)
                cardDao.insertAll(snapshot.cards)
                reviewLogDao.insertAll(snapshot.logs)
                for (tagId in snapshot.tagIds) {
                    if (tagDao.getById(tagId) != null) {
                        tagDao.link(DeckTagCrossRef(deck.id, tagId))
                    }
                }
            }
        }
    }

    suspend fun addNote(note: Note): Long = write {
        db.withTransaction {
            val noteId = noteDao.insert(note)
            val cards = mutableListOf(Card(noteId = noteId, ordinal = Card.ORDINAL_FORWARD))
            if (note.reverseEnabled) {
                cards.add(Card(noteId = noteId, ordinal = Card.ORDINAL_REVERSE))
            }
            cardDao.insertAll(cards)
            noteId
        }
    }

    /** Toggling reverse off deletes that card and its scheduling history. */
    suspend fun updateNote(note: Note, now: Long = System.currentTimeMillis()) = write {
        db.withTransaction {
            noteDao.update(note.copy(modifiedAt = now))
            val hasReverse = cardDao.getByNote(note.id).any { it.ordinal == Card.ORDINAL_REVERSE }
            if (note.reverseEnabled && !hasReverse) {
                cardDao.insert(Card(noteId = note.id, ordinal = Card.ORDINAL_REVERSE))
            } else if (!note.reverseEnabled && hasReverse) {
                cardDao.deleteByNoteAndOrdinal(note.id, Card.ORDINAL_REVERSE)
            }
        }
    }

    suspend fun deleteNote(note: Note): UndoAction {
        val snapshot = write {
            db.withTransaction {
                val cards = cardDao.getByNote(note.id)
                val logs = if (cards.isEmpty()) emptyList()
                else reviewLogDao.getByCards(cards.map { it.id })
                NoteSnapshot(cards, logs).also { noteDao.delete(note) }
            }
        }
        return UndoAction {
            restore {
                noteDao.insert(note)
                cardDao.insertAll(snapshot.cards)
                reviewLogDao.insertAll(snapshot.logs)
            }
        }
    }

    suspend fun setNoteSuspended(noteId: Long, suspended: Boolean) =
        write { cardDao.setNoteSuspended(noteId, suspended) }

    /** Learning cards come first because their intervals are minutes. */
    suspend fun buildStudyQueue(deckId: Long, now: Long): List<StudyCard> = write {
        db.withTransaction {
            val deck = deckDao.getById(deckId) ?: return@withTransaction emptyList<StudyCard>()

            // Daily caps are read back from the log, so reopening a session can't reset them.
            val dayStart = startOfTodayMillis(now)
            val reviewBudget = maxOf(
                0,
                deck.reviewsPerDay - reviewLogDao.countReviewsSince(deckId, dayStart),
            )
            val newBudget = maxOf(
                0,
                deck.newPerDay - reviewLogDao.countNewIntroducedSince(deckId, dayStart),
            )

            buildList {
                addAll(cardDao.findDueLearning(deckId, now, LEARNING_BATCH))
                addAll(cardDao.findDueReviews(deckId, now, reviewBudget))
                addAll(cardDao.findNew(deckId, newBudget))
            }
        }
    }

    /** The card update and log row are one transaction, so undo never sees a half-written review. */
    suspend fun answerCard(
        cardId: Long,
        rating: Rating,
        elapsedMs: Long,
        now: Long,
    ): AnswerResult? = write {
        db.withTransaction {
            val card = cardDao.getById(cardId) ?: return@withTransaction null
            val before = card.toSchedulingState()
            val after = scheduler.answer(before, rating, now)

            card.applySchedulingState(after)
            cardDao.update(card)

            val log = ReviewLog.of(cardId, rating.value, elapsedMs, before, after, now)
            AnswerResult(reviewLogDao.insert(log), after)
        }
    }

    /** Restores the logged snapshot and drops the log row. Returns the card id, or -1. */
    suspend fun undoLastReview(): Long = write {
        db.withTransaction {
            val log = reviewLogDao.findMostRecent() ?: return@withTransaction -1L
            val card = cardDao.getById(log.cardId)
            if (card != null) {
                card.applySchedulingState(log.toPreviousState())
                cardDao.update(card)
            }
            reviewLogDao.delete(log)
            log.cardId
        }
    }

    /** Null, writing nothing, if another tag has the name. Otherwise the saved tag's id. */
    suspend fun saveTag(tag: Tag): Long? = write {
        try {
            db.withTransaction {
                if (tagDao.findByNameExcluding(tag.name, tag.id) != null) {
                    return@withTransaction null
                }
                if (tag.id == 0L) {
                    tagDao.insert(tag).takeIf { it != -1L }
                } else {
                    tagDao.update(tag)
                    tag.id
                }
            }
        } catch (e: SQLiteConstraintException) {
            null
        }
    }

    suspend fun deleteTag(tag: Tag): UndoAction {
        val deckIds = write {
            db.withTransaction {
                tagDao.getDeckIdsForTag(tag.id).also { tagDao.delete(tag) }
            }
        }
        return UndoAction {
            restore {
                tagDao.insert(tag)
                for (deckId in deckIds) {
                    if (deckDao.getById(deckId) != null) {
                        tagDao.link(DeckTagCrossRef(deckId, tag.id))
                    }
                }
            }
        }
    }

    suspend fun setDeckTags(deckId: Long, tagIds: List<Long>) = write {
        db.withTransaction {
            tagDao.clearTagsForDeck(deckId)
            for (tagId in tagIds) {
                tagDao.link(DeckTagCrossRef(deckId, tagId))
            }
        }
    }

    suspend fun countDueEverywhere(now: Long): Int = io { cardDao.countDueEverywhere(now) }

    suspend fun reviewsSince(since: Long): Int = io { reviewLogDao.countSince(since) }

    suspend fun correctSince(since: Long): Int = io { reviewLogDao.countCorrectSince(since) }

    suspend fun timeSpentSince(since: Long): Long = io { reviewLogDao.sumTimeSpentSince(since) }

    suspend fun totalReviews(): Int = io { reviewLogDao.totalReviews() }

    suspend fun recentStudyDays(since: Long): List<String> =
        io { reviewLogDao.recentStudyDays(since) }

    suspend fun dailyCountsSince(since: Long): List<DailyCount> =
        io { reviewLogDao.dailyCountsSince(since) }

    suspend fun cardStateBreakdown(): CardStateBreakdown = io { cardDao.stateBreakdown() }

    private suspend fun <T> write(block: suspend () -> T): T =
        withContext(dispatchers.write) { block() }

    private suspend fun <T> io(block: suspend () -> T): T =
        withContext(dispatchers.io) { block() }

    /** One transaction on the write thread; rolls back if something the restore needs is gone. */
    private suspend fun restore(block: suspend () -> Unit): Boolean = write {
        try {
            db.withTransaction { block() }
            true
        } catch (e: SQLiteException) {
            false
        }
    }

    internal fun startOfTodayMillis(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, scheduler.config.rolloverHour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis > now) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    private class DeckSnapshot(
        val notes: List<Note>,
        val cards: List<Card>,
        val logs: List<ReviewLog>,
        val tagIds: List<Long>,
    )

    private class NoteSnapshot(
        val cards: List<Card>,
        val logs: List<ReviewLog>,
    )

    private companion object {
        const val LEARNING_BATCH = 50
    }
}

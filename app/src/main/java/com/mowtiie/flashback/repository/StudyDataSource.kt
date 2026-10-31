package com.mowtiie.flashback.repository

import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.model.StudyCard
import com.mowtiie.flashback.scheduler.Rating

/** The slice of persistence a study session needs, so the session can be tested with a fake. */
interface StudyDataSource {
    suspend fun getDeck(deckId: Long): Deck?

    suspend fun getNote(noteId: Long): Note?

    suspend fun getDeckNotes(deckId: Long): List<Note>

    suspend fun buildStudyQueue(deckId: Long, now: Long): List<StudyCard>

    suspend fun answerCard(
        cardId: Long,
        rating: Rating,
        elapsedMs: Long,
        now: Long,
    ): FlashbackRepository.AnswerResult?

    /** Returns the card id restored, or -1 when there was nothing to undo. */
    suspend fun undoLastReview(): Long

    suspend fun setNoteSuspended(noteId: Long, suspended: Boolean)
}

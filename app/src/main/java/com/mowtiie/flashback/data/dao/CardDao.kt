package com.mowtiie.flashback.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.data.model.StudyCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Insert
    suspend fun insert(card: Card): Long

    @Insert
    suspend fun insertAll(cards: List<Card>): List<Long>

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): Card?

    @Query("SELECT * FROM cards WHERE noteId = :noteId ORDER BY ordinal")
    suspend fun getByNote(noteId: Long): List<Card>

    @Query("SELECT c.* FROM cards c JOIN notes n ON c.noteId = n.id WHERE n.deckId = :deckId")
    suspend fun getByDeck(deckId: Long): List<Card>

    @Query("DELETE FROM cards WHERE noteId = :noteId AND ordinal = :ordinal")
    suspend fun deleteByNoteAndOrdinal(noteId: Long, ordinal: Int)

    @Query(
        "SELECT c.*, n.front AS front, n.back AS back FROM cards c " +
            "JOIN notes n ON c.noteId = n.id " +
            "WHERE n.deckId = :deckId AND c.suspended = 0 " +
            "AND c.state IN (1, 3) AND c.dueAt <= :now " +
            "ORDER BY c.dueAt LIMIT :limit",
    )
    suspend fun findDueLearning(deckId: Long, now: Long, limit: Int): List<StudyCard>

    @Query(
        "SELECT c.*, n.front AS front, n.back AS back FROM cards c " +
            "JOIN notes n ON c.noteId = n.id " +
            "WHERE n.deckId = :deckId AND c.suspended = 0 " +
            "AND c.state = 2 AND c.dueAt <= :now " +
            "ORDER BY c.dueAt LIMIT :limit",
    )
    suspend fun findDueReviews(deckId: Long, now: Long, limit: Int): List<StudyCard>

    @Query(
        "SELECT c.*, n.front AS front, n.back AS back FROM cards c " +
            "JOIN notes n ON c.noteId = n.id " +
            "WHERE n.deckId = :deckId AND c.suspended = 0 AND c.state = 0 " +
            "ORDER BY c.noteId, c.ordinal LIMIT :limit",
    )
    suspend fun findNew(deckId: Long, limit: Int): List<StudyCard>

    @Query("SELECT COUNT(*) FROM cards WHERE suspended = 0 AND state != 0 AND dueAt <= :now")
    suspend fun countDueEverywhere(now: Long): Int

    @Query("UPDATE cards SET suspended = :suspended WHERE noteId = :noteId")
    suspend fun setNoteSuspended(noteId: Long, suspended: Boolean)

    @Query("SELECT COUNT(*) FROM cards WHERE noteId = :noteId AND suspended = 1")
    suspend fun countSuspendedForNote(noteId: Long): Int

    @Query(
        "SELECT DISTINCT c.noteId FROM cards c JOIN notes n ON c.noteId = n.id " +
            "WHERE n.deckId = :deckId AND c.suspended = 1",
    )
    fun observeSuspendedNoteIds(deckId: Long): Flow<List<Long>>

    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN state = 0 THEN 1 ELSE 0 END), 0) AS newCount, " +
            "COALESCE(SUM(CASE WHEN state IN (1, 3) THEN 1 ELSE 0 END), 0) AS learningCount, " +
            "COALESCE(SUM(CASE WHEN state = 2 AND intervalDays < 21 THEN 1 ELSE 0 END), 0) AS youngCount, " +
            "COALESCE(SUM(CASE WHEN state = 2 AND intervalDays >= 21 THEN 1 ELSE 0 END), 0) AS matureCount " +
            "FROM cards WHERE suspended = 0",
    )
    suspend fun stateBreakdown(): CardStateBreakdown
}

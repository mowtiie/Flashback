package com.mowtiie.flashback.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.model.DeckSummary
import com.mowtiie.flashback.data.model.DeckWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {

    @Insert
    suspend fun insert(deck: Deck): Long

    @Update
    suspend fun update(deck: Deck)

    @Delete
    suspend fun delete(deck: Deck)

    @Query("SELECT * FROM decks WHERE id = :id")
    fun observeById(id: Long): Flow<Deck?>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getById(id: Long): Deck?

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Deck>

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Deck>>

    @Transaction
    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    fun observeAllWithTags(): Flow<List<DeckWithTags>>

    /** Counts come from grouped passes, not per-deck subqueries, which are much slower. */
    @Query(
        "SELECT d.*, " +
            "COALESCE(k.newCount, 0) AS newCount, " +
            "COALESCE(k.learnCount, 0) AS learnCount, " +
            "COALESCE(k.dueCount, 0) AS dueCount, " +
            "COALESCE(k.totalCount, 0) AS totalCount, " +
            "COALESCE(t.newToday, 0) AS newToday, " +
            "COALESCE(t.reviewsToday, 0) AS reviewsToday " +
            "FROM decks d " +
            "LEFT JOIN (SELECT n.deckId AS deckId, " +
            " SUM(c.suspended = 0 AND c.state = 0) AS newCount, " +
            " SUM(c.suspended = 0 AND c.state IN (1, 3) AND c.dueAt <= :now) AS learnCount, " +
            " SUM(c.suspended = 0 AND c.state = 2 AND c.dueAt <= :now) AS dueCount, " +
            " COUNT(DISTINCT c.noteId) AS totalCount " +
            " FROM cards c JOIN notes n ON c.noteId = n.id GROUP BY n.deckId) k " +
            "ON k.deckId = d.id " +
            "LEFT JOIN (SELECT n.deckId AS deckId, " +
            " SUM(r.prevState = 0) AS newToday, SUM(r.prevState = 2) AS reviewsToday " +
            " FROM review_log r JOIN cards c ON r.cardId = c.id " +
            " JOIN notes n ON c.noteId = n.id " +
            " WHERE r.reviewedAt >= :dayStart GROUP BY n.deckId) t " +
            "ON t.deckId = d.id " +
            "ORDER BY d.name COLLATE NOCASE",
    )
    fun observeSummaries(now: Long, dayStart: Long): Flow<List<DeckSummary>>

    @Query(
        "SELECT d.*, " +
            "COALESCE(k.newCount, 0) AS newCount, " +
            "COALESCE(k.learnCount, 0) AS learnCount, " +
            "COALESCE(k.dueCount, 0) AS dueCount, " +
            "COALESCE(k.totalCount, 0) AS totalCount, " +
            "COALESCE(t.newToday, 0) AS newToday, " +
            "COALESCE(t.reviewsToday, 0) AS reviewsToday " +
            "FROM decks d " +
            "LEFT JOIN (SELECT n.deckId AS deckId, " +
            " SUM(c.suspended = 0 AND c.state = 0) AS newCount, " +
            " SUM(c.suspended = 0 AND c.state IN (1, 3) AND c.dueAt <= :now) AS learnCount, " +
            " SUM(c.suspended = 0 AND c.state = 2 AND c.dueAt <= :now) AS dueCount, " +
            " COUNT(DISTINCT c.noteId) AS totalCount " +
            " FROM cards c JOIN notes n ON c.noteId = n.id " +
            " WHERE n.deckId = :deckId GROUP BY n.deckId) k " +
            "ON k.deckId = d.id " +
            "LEFT JOIN (SELECT n.deckId AS deckId, " +
            " SUM(r.prevState = 0) AS newToday, SUM(r.prevState = 2) AS reviewsToday " +
            " FROM review_log r JOIN cards c ON r.cardId = c.id " +
            " JOIN notes n ON c.noteId = n.id " +
            " WHERE n.deckId = :deckId AND r.reviewedAt >= :dayStart GROUP BY n.deckId) t " +
            "ON t.deckId = d.id " +
            "WHERE d.id = :deckId",
    )
    fun observeSummary(deckId: Long, now: Long, dayStart: Long): Flow<DeckSummary?>
}

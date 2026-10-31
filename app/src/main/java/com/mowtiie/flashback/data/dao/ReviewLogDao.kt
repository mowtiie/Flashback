package com.mowtiie.flashback.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.mowtiie.flashback.data.entity.ReviewLog
import com.mowtiie.flashback.data.model.DailyCount

@Dao
interface ReviewLogDao {

    @Insert
    suspend fun insert(log: ReviewLog): Long

    @Insert
    suspend fun insertAll(logs: List<ReviewLog>)

    @Query(
        "SELECT r.* FROM review_log r JOIN cards c ON r.cardId = c.id " +
            "JOIN notes n ON c.noteId = n.id WHERE n.deckId = :deckId",
    )
    suspend fun getByDeck(deckId: Long): List<ReviewLog>

    @Query("SELECT * FROM review_log WHERE cardId IN (:cardIds)")
    suspend fun getByCards(cardIds: List<Long>): List<ReviewLog>

    @Delete
    suspend fun delete(log: ReviewLog)

    @Query("SELECT * FROM review_log ORDER BY reviewedAt DESC, id DESC LIMIT 1")
    suspend fun findMostRecent(): ReviewLog?

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :since")
    suspend fun countSince(since: Long): Int

    @Query(
        "SELECT COUNT(*) FROM review_log r JOIN cards c ON r.cardId = c.id " +
            "JOIN notes n ON c.noteId = n.id " +
            "WHERE n.deckId = :deckId AND r.reviewedAt >= :since AND r.prevState = 0",
    )
    suspend fun countNewIntroducedSince(deckId: Long, since: Long): Int

    @Query(
        "SELECT COUNT(*) FROM review_log r JOIN cards c ON r.cardId = c.id " +
            "JOIN notes n ON c.noteId = n.id " +
            "WHERE n.deckId = :deckId AND r.reviewedAt >= :since AND r.prevState = 2",
    )
    suspend fun countReviewsSince(deckId: Long, since: Long): Int

    @Query("SELECT COALESCE(SUM(elapsedMs), 0) FROM review_log WHERE reviewedAt >= :since")
    suspend fun sumTimeSpentSince(since: Long): Long

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :since AND rating > 1")
    suspend fun countCorrectSince(since: Long): Int

    @Query(
        "SELECT date(reviewedAt / 1000, 'unixepoch', 'localtime') AS day, " +
            "COUNT(*) AS count FROM review_log " +
            "WHERE reviewedAt >= :since GROUP BY day ORDER BY day",
    )
    suspend fun dailyCountsSince(since: Long): List<DailyCount>

    @Query("SELECT COUNT(*) FROM review_log")
    suspend fun totalReviews(): Int

    /** Bounded by the reviewedAt index, so cost doesn't grow with history. */
    @Query(
        "SELECT DISTINCT date(reviewedAt / 1000, 'unixepoch', 'localtime') AS day " +
            "FROM review_log WHERE reviewedAt >= :since ORDER BY day DESC LIMIT 400",
    )
    suspend fun recentStudyDays(since: Long): List<String>
}

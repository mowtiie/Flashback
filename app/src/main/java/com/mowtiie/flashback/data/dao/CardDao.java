package com.mowtiie.flashback.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mowtiie.flashback.data.entity.Card;
import com.mowtiie.flashback.data.model.CardStateBreakdown;
import com.mowtiie.flashback.data.model.StudyCard;

import java.util.List;

@Dao
public interface CardDao {

    @Insert
    long insert(Card card);

    @Insert
    List<Long> insertAll(List<Card> cards);

    @Update
    void update(Card card);

    @Delete
    void delete(Card card);

    @Query("SELECT * FROM cards WHERE id = :id")
    Card getById(long id);

    @Query("SELECT * FROM cards WHERE noteId = :noteId ORDER BY ordinal")
    List<Card> getByNote(long noteId);

    @Query("DELETE FROM cards WHERE noteId = :noteId AND ordinal = :ordinal")
    void deleteByNoteAndOrdinal(long noteId, int ordinal);

    /**
     * Cards already in a learning phase and ready now. These come first in a
     * session because their intervals are measured in minutes.
     */
    @Query("SELECT c.*, n.front AS front, n.back AS back FROM cards c "
            + "JOIN notes n ON c.noteId = n.id "
            + "WHERE n.deckId = :deckId AND c.suspended = 0 "
            + "AND c.state IN (1, 3) AND c.dueAt <= :now "
            + "ORDER BY c.dueAt LIMIT :limit")
    List<StudyCard> findDueLearning(long deckId, long now, int limit);

    /** Graduated cards whose due date has arrived. */
    @Query("SELECT c.*, n.front AS front, n.back AS back FROM cards c "
            + "JOIN notes n ON c.noteId = n.id "
            + "WHERE n.deckId = :deckId AND c.suspended = 0 "
            + "AND c.state = 2 AND c.dueAt <= :now "
            + "ORDER BY c.dueAt LIMIT :limit")
    List<StudyCard> findDueReviews(long deckId, long now, int limit);

    /** Unseen cards, introduced up to the deck's daily allowance. */
    @Query("SELECT c.*, n.front AS front, n.back AS back FROM cards c "
            + "JOIN notes n ON c.noteId = n.id "
            + "WHERE n.deckId = :deckId AND c.suspended = 0 AND c.state = 0 "
            + "ORDER BY c.noteId, c.ordinal LIMIT :limit")
    List<StudyCard> findNew(long deckId, int limit);

    /** Backs the notification worker's "cards waiting" figure. */
    @Query("SELECT COUNT(*) FROM cards WHERE suspended = 0 AND state != 0 AND dueAt <= :now")
    int countDueEverywhere(long now);

    @Query("SELECT COUNT(*) FROM cards c JOIN notes n ON c.noteId = n.id "
            + "WHERE n.deckId = :deckId AND c.suspended = 0 "
            + "AND c.state != 0 AND c.dueAt <= :now")
    int countDueInDeck(long deckId, long now);

    @Query("UPDATE cards SET suspended = :suspended WHERE id = :cardId")
    void setSuspended(long cardId, boolean suspended);

    /** One-row maturity breakdown for the statistics screen. Excludes suspended. */
    @Query("SELECT "
            + "SUM(CASE WHEN state = 0 THEN 1 ELSE 0 END) AS newCount, "
            + "SUM(CASE WHEN state IN (1, 3) THEN 1 ELSE 0 END) AS learningCount, "
            + "SUM(CASE WHEN state = 2 AND intervalDays < 21 THEN 1 ELSE 0 END) AS youngCount, "
            + "SUM(CASE WHEN state = 2 AND intervalDays >= 21 THEN 1 ELSE 0 END) AS matureCount "
            + "FROM cards WHERE suspended = 0")
    CardStateBreakdown stateBreakdown();
}

package com.mowtiie.flashback.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.model.DeckSummary;
import com.mowtiie.flashback.data.model.DeckWithTags;

import java.util.List;

@Dao
public interface DeckDao {

    @Insert
    long insert(Deck deck);

    @Update
    void update(Deck deck);

    @Delete
    void delete(Deck deck);

    @Query("SELECT * FROM decks WHERE id = :id")
    LiveData<Deck> observeById(long id);

    @Query("SELECT * FROM decks WHERE id = :id")
    Deck getById(long id);

    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    LiveData<List<Deck>> observeAll();

    @Transaction
    @Query("SELECT * FROM decks ORDER BY name COLLATE NOCASE")
    LiveData<List<DeckWithTags>> observeAllWithTags();

    @Query("SELECT d.*, "
            + "(SELECT COUNT(*) FROM cards c JOIN notes n ON c.noteId = n.id "
            + " WHERE n.deckId = d.id AND c.suspended = 0 AND c.state = 0) AS newCount, "
            + "(SELECT COUNT(*) FROM cards c JOIN notes n ON c.noteId = n.id "
            + " WHERE n.deckId = d.id AND c.suspended = 0 AND c.state IN (1, 3) "
            + " AND c.dueAt <= :now) AS learnCount, "
            + "(SELECT COUNT(*) FROM cards c JOIN notes n ON c.noteId = n.id "
            + " WHERE n.deckId = d.id AND c.suspended = 0 AND c.state = 2 "
            + " AND c.dueAt <= :now) AS dueCount, "
            + "(SELECT COUNT(*) FROM cards c JOIN notes n ON c.noteId = n.id "
            + " WHERE n.deckId = d.id) AS totalCount "
            + "FROM decks d ORDER BY d.name COLLATE NOCASE")
    LiveData<List<DeckSummary>> observeSummaries(long now);

    @Query("SELECT COUNT(*) FROM decks")
    int count();
}

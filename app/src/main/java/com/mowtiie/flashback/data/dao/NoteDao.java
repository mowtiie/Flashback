package com.mowtiie.flashback.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mowtiie.flashback.data.entity.Note;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    long insert(Note note);

    @Insert
    List<Long> insertAll(List<Note> notes);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("SELECT * FROM notes WHERE id = :id")
    Note getById(long id);

    @Query("SELECT * FROM notes WHERE deckId = :deckId ORDER BY createdAt DESC")
    LiveData<List<Note>> observeByDeck(long deckId);

    @Query("SELECT * FROM notes WHERE deckId = :deckId ORDER BY createdAt")
    List<Note> getByDeck(long deckId);

    @Query("SELECT COUNT(*) FROM notes WHERE deckId = :deckId")
    int countInDeck(long deckId);
}

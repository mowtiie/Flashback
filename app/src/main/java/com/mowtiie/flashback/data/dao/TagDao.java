package com.mowtiie.flashback.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mowtiie.flashback.data.entity.DeckTagCrossRef;
import com.mowtiie.flashback.data.entity.Tag;

import java.util.List;

@Dao
public interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Tag tag);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    LiveData<List<Tag>> observeAll();

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    Tag findByName(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void link(DeckTagCrossRef ref);

    @Delete
    void unlink(DeckTagCrossRef ref);

    @Query("DELETE FROM deck_tag WHERE deckId = :deckId")
    void clearTagsForDeck(long deckId);

    @Query("SELECT t.* FROM tags t JOIN deck_tag dt ON t.id = dt.tagId "
            + "WHERE dt.deckId = :deckId ORDER BY t.name COLLATE NOCASE")
    LiveData<List<Tag>> observeTagsForDeck(long deckId);

    @Query("SELECT COUNT(*) FROM deck_tag WHERE tagId = :tagId")
    int deckCountForTag(long tagId);
}

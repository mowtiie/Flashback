package com.mowtiie.flashback.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mowtiie.flashback.data.entity.DeckTagCrossRef
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): Tag?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Query("SELECT * FROM tags WHERE name = :name AND id != :excludeId LIMIT 1")
    suspend fun findByNameExcluding(name: String, excludeId: Long): Tag?

    @Query(
        "SELECT t.*, " +
            "(SELECT COUNT(*) FROM deck_tag dt WHERE dt.tagId = t.id) AS deckCount " +
            "FROM tags t ORDER BY t.name COLLATE NOCASE",
    )
    fun observeAllWithCounts(): Flow<List<TagWithCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(ref: DeckTagCrossRef)

    @Query("SELECT tagId FROM deck_tag WHERE deckId = :deckId")
    suspend fun getTagIdsForDeck(deckId: Long): List<Long>

    @Query("SELECT deckId FROM deck_tag WHERE tagId = :tagId")
    suspend fun getDeckIdsForTag(tagId: Long): List<Long>

    @Query("DELETE FROM deck_tag WHERE deckId = :deckId")
    suspend fun clearTagsForDeck(deckId: Long)

    @Query(
        "SELECT t.* FROM tags t JOIN deck_tag dt ON t.id = dt.tagId " +
            "WHERE dt.deckId = :deckId ORDER BY t.name COLLATE NOCASE",
    )
    fun observeTagsForDeck(deckId: Long): Flow<List<Tag>>

    @Query(
        "SELECT t.* FROM tags t JOIN deck_tag dt ON t.id = dt.tagId " +
            "WHERE dt.deckId = :deckId ORDER BY t.name COLLATE NOCASE",
    )
    suspend fun getTagsForDeck(deckId: Long): List<Tag>
}

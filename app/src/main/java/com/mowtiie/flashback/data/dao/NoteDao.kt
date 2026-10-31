package com.mowtiie.flashback.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.model.NotePreview
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: Note): Long

    @Insert
    suspend fun insertAll(notes: List<Note>): List<Long>

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    /** Text is cut to 200 characters: the list shows two lines and edits load the full note. */
    @Query(
        "SELECT id, substr(front, 1, 200) AS front, substr(back, 1, 200) AS back, " +
            "reverseEnabled FROM notes WHERE deckId = :deckId ORDER BY createdAt DESC",
    )
    fun observeByDeck(deckId: Long): Flow<List<NotePreview>>

    @Query("SELECT * FROM notes WHERE deckId = :deckId ORDER BY createdAt")
    suspend fun getByDeck(deckId: Long): List<Note>
}

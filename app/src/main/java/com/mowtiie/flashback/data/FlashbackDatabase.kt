package com.mowtiie.flashback.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mowtiie.flashback.data.dao.CardDao
import com.mowtiie.flashback.data.dao.DeckDao
import com.mowtiie.flashback.data.dao.NoteDao
import com.mowtiie.flashback.data.dao.ReviewLogDao
import com.mowtiie.flashback.data.dao.TagDao
import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.DeckTagCrossRef
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.entity.ReviewLog
import com.mowtiie.flashback.data.entity.Tag

@Database(
    entities = [
        Deck::class,
        Note::class,
        Card::class,
        Tag::class,
        DeckTagCrossRef::class,
        ReviewLog::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FlashbackDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao

    abstract fun noteDao(): NoteDao

    abstract fun cardDao(): CardDao

    abstract fun tagDao(): TagDao

    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        private const val DB_NAME = "flashback.db"

        @Volatile
        private var instance: FlashbackDatabase? = null

        fun getInstance(context: Context): FlashbackDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlashbackDatabase::class.java,
                    DB_NAME,
                )
                    // Lets the study screen read while a write is in flight.
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instance = it }
            }
    }
}

package com.mowtiie.flashback.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mowtiie.flashback.data.dao.CardDao;
import com.mowtiie.flashback.data.dao.DeckDao;
import com.mowtiie.flashback.data.dao.NoteDao;
import com.mowtiie.flashback.data.dao.ReviewLogDao;
import com.mowtiie.flashback.data.dao.TagDao;
import com.mowtiie.flashback.data.entity.Card;
import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.entity.DeckTagCrossRef;
import com.mowtiie.flashback.data.entity.Note;
import com.mowtiie.flashback.data.entity.ReviewLog;
import com.mowtiie.flashback.data.entity.Tag;

@Database(
        entities = {
                Deck.class,
                Note.class,
                Card.class,
                Tag.class,
                DeckTagCrossRef.class,
                ReviewLog.class
        },
        version = 1,
        exportSchema = true)
public abstract class FlashbackDatabase extends RoomDatabase {

    private static final String DB_NAME = "flashback.db";

    private static volatile FlashbackDatabase instance;

    public abstract DeckDao deckDao();

    public abstract NoteDao noteDao();

    public abstract CardDao cardDao();

    public abstract TagDao tagDao();

    public abstract ReviewLogDao reviewLogDao();

    public static FlashbackDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (FlashbackDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    FlashbackDatabase.class,
                                    DB_NAME)
                            // Lets the study screen read while a write is in flight.
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .build();
                }
            }
        }
        return instance;
    }
}

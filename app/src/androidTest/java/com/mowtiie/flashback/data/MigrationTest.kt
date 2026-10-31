package com.mowtiie.flashback.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Java app's last release is schema 1, so this is the upgrade path existing installs take.
 * The old database is built from the exported 1.json, then opened by the real Room builder, which
 * runs the migration and validates the result against the current entities.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var db: FlashbackDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(DB)
    }

    @After
    fun tearDown() {
        db?.close()
        context.deleteDatabase(DB)
    }

    private fun createV1WithData() {
        val schema = JSONObject(
            InstrumentationRegistry.getInstrumentation().context.assets
                .open("com.mowtiie.flashback.data.FlashbackDatabase/1.json").bufferedReader().use { it.readText() },
        ).getJSONObject("database")

        val file = context.getDatabasePath(DB).also { it.parentFile?.mkdirs() }
        val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
        raw.use {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                it.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    it.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            it.execSQL(
                "INSERT INTO decks (id, name, description, reverseByDefault, newPerDay, reviewsPerDay, createdAt) " +
                    "VALUES (1, 'Spanish', NULL, 0, 20, 200, 1000)",
            )
            it.execSQL(
                "INSERT INTO notes (id, deckId, front, back, reverseEnabled, createdAt, modifiedAt) " +
                    "VALUES (1, 1, 'hola', 'hello', 1, 1000, 1000), (2, 1, 'adios', 'goodbye', 0, 1000, 1000)",
            )
            it.execSQL(
                "INSERT INTO cards (id, noteId, ordinal, state, learningStep, intervalDays, easeFactor, reps, " +
                    "lapses, dueAt, lastReviewedAt, suspended) VALUES (1, 1, 0, 2, 0, 12, 2.5, 4, 1, 5000, 4000, 0)",
            )
            it.version = 1
        }
    }

    private fun openUpgraded(): FlashbackDatabase =
        Room.databaseBuilder(context, FlashbackDatabase::class.java, DB)
            .addMigrations(MIGRATION_1_2)
            .build()
            .also { db = it }

    @Test
    fun upgradingKeepsEveryExistingRow() {
        createV1WithData()
        val sqlite = openUpgraded().openHelper.writableDatabase

        sqlite.query("SELECT front, back, reverseEnabled FROM notes ORDER BY id").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals("hola", cursor.getString(0))
            assertEquals("hello", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
        }
        sqlite.query("SELECT intervalDays, easeFactor, reps, lapses FROM cards WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(12, cursor.getInt(0))
            assertEquals(2.5, cursor.getDouble(1), 0.0)
            assertEquals(4, cursor.getInt(2))
            assertEquals(1, cursor.getInt(3))
        }
    }

    @Test
    fun existingNotesFollowTheStudySettingAfterTheUpgrade() {
        createV1WithData()
        val sqlite = openUpgraded().openHelper.writableDatabase

        sqlite.query("SELECT answerMode, choices FROM notes").use { cursor ->
            assertTrue(cursor.moveToFirst())
            do {
                assertTrue(cursor.isNull(0))
                assertTrue(cursor.isNull(1))
            } while (cursor.moveToNext())
        }
    }

    @Test
    fun theUpgradedDatabaseWorksThroughTheDaos() = kotlinx.coroutines.runBlocking {
        createV1WithData()
        val database = openUpgraded()

        val notes = database.noteDao().getByDeck(1)
        assertEquals(listOf("hola", "adios"), notes.map { it.front })
        assertEquals(null, notes.first().answerMode)

        database.noteDao().update(notes.last().copy(answerMode = 2, choices = "[\"Rome\"]"))
        val saved = database.noteDao().getByDeck(1).last()
        assertEquals(2, saved.answerMode)
        assertEquals("[\"Rome\"]", saved.choices)
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}

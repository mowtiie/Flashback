package com.mowtiie.flashback.data.io

import androidx.room.withTransaction
import com.mowtiie.flashback.data.FlashbackDatabase
import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.DeckTagCrossRef
import com.mowtiie.flashback.data.entity.Note
import com.mowtiie.flashback.data.entity.Tag

class ImportResult(
    val decks: Int,
    val notes: Int,
    val tagsCreated: Int,
    val tagsReused: Int,
)

/** Archive read/write across the whole graph. The caller picks the dispatcher. */
class ArchiveRepository(private val db: FlashbackDatabase) {

    suspend fun exportAll(now: Long): DeckArchive =
        DeckArchive(exportedAt = now, decks = db.deckDao().getAll().map { exportDeck(it) })

    suspend fun exportSingle(deckId: Long, now: Long): DeckArchive {
        val deck = db.deckDao().getById(deckId)
        return DeckArchive(exportedAt = now, decks = listOfNotNull(deck).map { exportDeck(it) })
    }

    private suspend fun exportDeck(deck: Deck) = DeckArchive.Deck(
        name = deck.name,
        description = deck.description,
        reverseByDefault = deck.reverseByDefault,
        newPerDay = deck.newPerDay,
        reviewsPerDay = deck.reviewsPerDay,
        tags = db.tagDao().getTagsForDeck(deck.id).map { DeckArchive.Tag(it.name, it.description, it.color) },
        notes = db.noteDao().getByDeck(deck.id).map { DeckArchive.Note(it.front, it.back, it.reverseEnabled) },
    )

    /** Writes an archive in one transaction. Tags merge by name. */
    suspend fun importArchive(archive: DeckArchive): ImportResult = db.withTransaction {
        var noteCount = 0
        var tagsCreated = 0
        var tagsReused = 0

        for (source in archive.decks) {
            val deckId = db.deckDao().insert(
                Deck(
                    name = source.name,
                    description = source.description,
                    reverseByDefault = source.reverseByDefault,
                    newPerDay = source.newPerDay,
                    reviewsPerDay = source.reviewsPerDay,
                ),
            )

            for (sourceTag in source.tags) {
                val existing = db.tagDao().findByName(sourceTag.name)
                val tagId = if (existing != null) {
                    tagsReused++
                    existing.id
                } else {
                    tagsCreated++
                    db.tagDao().insert(Tag(name = sourceTag.name, description = sourceTag.description, color = sourceTag.color))
                }
                db.tagDao().link(DeckTagCrossRef(deckId, tagId))
            }

            insertNotesWithCards(deckId, source.notes)
            noteCount += source.notes.size
        }

        ImportResult(archive.decks.size, noteCount, tagsCreated, tagsReused)
    }

    private suspend fun insertNotesWithCards(deckId: Long, sources: List<DeckArchive.Note>) {
        val notes = sources.map { Note(deckId = deckId, front = it.front, back = it.back, reverseEnabled = it.reverseEnabled) }
        val noteIds = db.noteDao().insertAll(notes)
        val cards = buildList {
            notes.forEachIndexed { index, note ->
                add(Card(noteId = noteIds[index], ordinal = Card.ORDINAL_FORWARD))
                if (note.reverseEnabled) add(Card(noteId = noteIds[index], ordinal = Card.ORDINAL_REVERSE))
            }
        }
        db.cardDao().insertAll(cards)
    }
}

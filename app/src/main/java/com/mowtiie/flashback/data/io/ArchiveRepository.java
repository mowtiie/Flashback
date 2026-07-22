package com.mowtiie.flashback.data.io;

import androidx.annotation.WorkerThread;

import com.mowtiie.flashback.data.FlashbackDatabase;
import com.mowtiie.flashback.data.entity.Card;
import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.entity.DeckTagCrossRef;
import com.mowtiie.flashback.data.entity.Note;
import com.mowtiie.flashback.data.entity.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads decks out to a {@link DeckArchive} and writes one back in. Kept apart
 * from FlashbackRepository because this is the only code that walks the whole
 * graph at once, and all of it must run inside a transaction.
 *
 * <p>Every method here is blocking and must be called on a background thread.
 * The UI layer wraps these in the shared disk executor.
 */
public class ArchiveRepository {

    private final FlashbackDatabase db;

    public ArchiveRepository(FlashbackDatabase db) {
        this.db = db;
    }

    // ----------------------------------------------------------- export

    @WorkerThread
    public DeckArchive exportAll() {
        DeckArchive archive = new DeckArchive();
        archive.exportedAt = System.currentTimeMillis();
        for (Deck deck : db.deckDao().getAll()) {
            archive.decks.add(exportDeck(deck));
        }
        return archive;
    }

    @WorkerThread
    public DeckArchive exportSingle(long deckId) {
        DeckArchive archive = new DeckArchive();
        archive.exportedAt = System.currentTimeMillis();
        Deck deck = db.deckDao().getById(deckId);
        if (deck != null) {
            archive.decks.add(exportDeck(deck));
        }
        return archive;
    }

    private DeckArchive.Deck exportDeck(Deck deck) {
        DeckArchive.Deck out = new DeckArchive.Deck();
        out.name = deck.name;
        out.description = deck.description;
        out.reverseByDefault = deck.reverseByDefault;
        out.newPerDay = deck.newPerDay;
        out.reviewsPerDay = deck.reviewsPerDay;

        for (Tag tag : db.tagDao().getTagsForDeck(deck.id)) {
            DeckArchive.Tag outTag = new DeckArchive.Tag();
            outTag.name = tag.name;
            outTag.description = tag.description;
            outTag.color = tag.color;
            out.tags.add(outTag);
        }

        for (Note note : db.noteDao().getByDeck(deck.id)) {
            DeckArchive.Note outNote = new DeckArchive.Note();
            outNote.front = note.front;
            outNote.back = note.back;
            outNote.reverseEnabled = note.reverseEnabled;
            out.notes.add(outNote);
        }
        return out;
    }

    // ----------------------------------------------------------- import

    /** What an import actually wrote, for the confirmation message. */
    public static class ImportResult {
        public final int decks;
        public final int notes;
        public final int tagsCreated;
        public final int tagsReused;

        ImportResult(int decks, int notes, int tagsCreated, int tagsReused) {
            this.decks = decks;
            this.notes = notes;
            this.tagsCreated = tagsCreated;
            this.tagsReused = tagsReused;
        }
    }

    /**
     * Writes an archive into the database in a single transaction. A failure
     * rolls the whole thing back, so a malformed archive can never leave a
     * half-imported deck. Imported cards start fresh in the NEW state.
     *
     * <p>Tags merge by name: an imported tag whose name already exists links to
     * the existing tag rather than creating a duplicate, which matches the
     * unique index on tag names.
     */
    @WorkerThread
    public ImportResult importArchive(DeckArchive archive) {
        return db.runInTransaction(() -> {
            int deckCount = 0;
            int noteCount = 0;
            int[] tagStats = {0, 0}; // created, reused

            for (DeckArchive.Deck source : archive.decks) {
                Deck deck = new Deck(source.name, source.description);
                deck.reverseByDefault = source.reverseByDefault;
                deck.newPerDay = source.newPerDay;
                deck.reviewsPerDay = source.reviewsPerDay;
                long deckId = db.deckDao().insert(deck);
                deckCount++;

                for (DeckArchive.Tag sourceTag : source.tags) {
                    long tagId = resolveTag(sourceTag, tagStats);
                    db.tagDao().link(new DeckTagCrossRef(deckId, tagId));
                }

                List<Note> notes = new ArrayList<>(source.notes.size());
                for (DeckArchive.Note sourceNote : source.notes) {
                    notes.add(new Note(deckId, sourceNote.front, sourceNote.back,
                            sourceNote.reverseEnabled));
                }
                insertNotesWithCards(notes);
                noteCount += notes.size();
            }

            return new ImportResult(deckCount, noteCount, tagStats[0], tagStats[1]);
        });
    }

    /** Finds an existing tag by name or creates one; records which happened. */
    private long resolveTag(DeckArchive.Tag sourceTag, int[] tagStats) {
        Tag existing = db.tagDao().findByName(sourceTag.name);
        if (existing != null) {
            tagStats[1]++;
            return existing.id;
        }
        Tag tag = new Tag(sourceTag.name, sourceTag.description, sourceTag.color);
        long id = db.tagDao().insert(tag);
        tagStats[0]++;
        return id;
    }

    /** Mirrors the note-plus-cards rule from FlashbackRepository. */
    private void insertNotesWithCards(List<Note> notes) {
        for (Note note : notes) {
            long noteId = db.noteDao().insert(note);
            List<Card> cards = new ArrayList<>(2);
            cards.add(new Card(noteId, Card.ORDINAL_FORWARD));
            if (note.reverseEnabled) {
                cards.add(new Card(noteId, Card.ORDINAL_REVERSE));
            }
            db.cardDao().insertAll(cards);
        }
    }
}

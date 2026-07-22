package com.mowtiie.flashback.data.io;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-memory shape of an import/export file. Plain POJOs with no Room or
 * Android types, so {@link ArchiveSerializer} and {@link ArchiveParser} can be
 * round-tripped in JVM tests.
 *
 * <p>The format is content only: decks, their tags, and their notes. Scheduling
 * state (intervals, ease, due dates) is deliberately left out. A shared deck
 * should arrive fresh for the person importing it, not carrying the exporter's
 * review history, and the Gemini path in a later phase produces content with no
 * schedule to carry. A future v2 can add an optional backup mode that includes
 * state; the version field is what makes that expansion safe.
 */
public class DeckArchive {

    /** Bumped only on a breaking change. The parser rejects anything higher. */
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;

    public long exportedAt;

    public List<Deck> decks = new ArrayList<>();

    public static class Deck {
        public String name;
        public String description;
        public boolean reverseByDefault;
        public int newPerDay = 20;
        public int reviewsPerDay = 200;
        public List<Tag> tags = new ArrayList<>();
        public List<Note> notes = new ArrayList<>();
    }

    public static class Tag {
        public String name;
        public String description;
        public int color;
    }

    public static class Note {
        public String front;
        public String back;
        public boolean reverseEnabled;
    }

    /** Totals for the import preview, so the user confirms before anything writes. */
    public Counts counts() {
        int noteCount = 0;
        int tagCount = 0;
        for (Deck deck : decks) {
            noteCount += deck.notes.size();
            tagCount += deck.tags.size();
        }
        return new Counts(decks.size(), noteCount, tagCount);
    }

    public static class Counts {
        public final int decks;
        public final int notes;
        public final int tags;

        Counts(int decks, int notes, int tags) {
            this.decks = decks;
            this.notes = notes;
            this.tags = tags;
        }
    }
}

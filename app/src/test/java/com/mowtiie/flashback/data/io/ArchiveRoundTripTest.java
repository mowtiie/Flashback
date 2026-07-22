package com.mowtiie.flashback.data.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * Runs on the JVM against the real org.json (see the testImplementation entry
 * in build.gradle.kts). Android bundles org.json at runtime, so no production
 * dependency is added.
 */
public class ArchiveRoundTripTest {

    private ArchiveSerializer serializer;
    private ArchiveParser parser;

    @Before
    public void setUp() {
        serializer = new ArchiveSerializer();
        parser = new ArchiveParser();
    }

    private DeckArchive sample() {
        DeckArchive archive = new DeckArchive();
        archive.exportedAt = 1_700_000_000_000L;

        DeckArchive.Deck deck = new DeckArchive.Deck();
        deck.name = "Spanish";
        deck.description = "Core vocab";
        deck.reverseByDefault = true;
        deck.newPerDay = 15;
        deck.reviewsPerDay = 150;

        DeckArchive.Tag tag = new DeckArchive.Tag();
        tag.name = "Language";
        tag.color = -12345;
        deck.tags.add(tag);

        DeckArchive.Note a = new DeckArchive.Note();
        a.front = "el bosque";
        a.back = "the forest";
        a.reverseEnabled = true;
        DeckArchive.Note b = new DeckArchive.Note();
        b.front = "la montaña";
        b.back = "the mountain";
        deck.notes.addAll(Arrays.asList(a, b));

        archive.decks.add(deck);
        return archive;
    }

    @Test
    public void roundTripPreservesEveryField() throws Exception {
        DeckArchive back = parser.parse(serializer.serialize(sample()));

        assertEquals(1, back.version);
        assertEquals(1_700_000_000_000L, back.exportedAt);
        assertEquals(1, back.decks.size());

        DeckArchive.Deck deck = back.decks.get(0);
        assertEquals("Spanish", deck.name);
        assertEquals("Core vocab", deck.description);
        assertTrue(deck.reverseByDefault);
        assertEquals(15, deck.newPerDay);
        assertEquals(150, deck.reviewsPerDay);
        assertEquals("Language", deck.tags.get(0).name);
        assertEquals(-12345, deck.tags.get(0).color);
        assertEquals(2, deck.notes.size());
        assertTrue(deck.notes.get(0).reverseEnabled);
        assertFalse(deck.notes.get(1).reverseEnabled);
    }

    @Test
    public void countsMatchContents() throws Exception {
        DeckArchive.Counts counts = parser.parse(serializer.serialize(sample())).counts();
        assertEquals(1, counts.decks);
        assertEquals(2, counts.notes);
        assertEquals(1, counts.tags);
    }

    @Test
    public void optionalFieldsFallBackToDefaults() throws Exception {
        DeckArchive archive = parser.parse(
                "{\"version\":1,\"decks\":[{\"name\":\"D\",\"notes\":[]}]}");
        DeckArchive.Deck deck = archive.decks.get(0);
        assertEquals(20, deck.newPerDay);
        assertEquals(200, deck.reviewsPerDay);
        assertNull(deck.description);
    }

    @Test
    public void namelessTagIsDroppedNotFatal() throws Exception {
        DeckArchive archive = parser.parse("{\"version\":1,\"decks\":[{\"name\":\"D\","
                + "\"tags\":[{\"color\":1},{\"name\":\"Keep\"}],\"notes\":[]}]}");
        assertEquals(1, archive.decks.get(0).tags.size());
        assertEquals("Keep", archive.decks.get(0).tags.get(0).name);
    }

    @Test
    public void unicodeAndEscapesSurvive() throws Exception {
        DeckArchive archive = new DeckArchive();
        DeckArchive.Deck deck = new DeckArchive.Deck();
        deck.name = "Quotes \" and \\ and \n and 日本語";
        DeckArchive.Note note = new DeckArchive.Note();
        note.front = "emoji 🎴";
        note.back = "tab\there";
        deck.notes.add(note);
        archive.decks.add(deck);

        DeckArchive back = parser.parse(serializer.serialize(archive));
        assertEquals(deck.name, back.decks.get(0).name);
        assertEquals("emoji 🎴", back.decks.get(0).notes.get(0).front);
        assertEquals("tab\there", back.decks.get(0).notes.get(0).back);
    }

    private void assertRejected(String raw, String fragment) {
        try {
            parser.parse(raw);
            fail("Expected rejection for: " + raw);
        } catch (ArchiveException e) {
            assertTrue("message was: " + e.getMessage(),
                    e.getMessage().toLowerCase().contains(fragment.toLowerCase()));
        }
    }

    @Test
    public void emptyInputRejected() {
        assertRejected("", "empty");
    }

    @Test
    public void nonJsonRejected() {
        assertRejected("not json at all", "valid json");
    }

    @Test
    public void missingVersionRejected() {
        assertRejected("{\"decks\":[]}", "version");
    }

    @Test
    public void futureVersionRejected() {
        assertRejected("{\"version\":99,\"decks\":[{\"name\":\"x\"}]}", "newer version");
    }

    @Test
    public void missingDecksRejected() {
        assertRejected("{\"version\":1}", "no decks");
    }

    @Test
    public void emptyDecksRejected() {
        assertRejected("{\"version\":1,\"decks\":[]}", "no decks");
    }

    @Test
    public void deckWithoutNameRejected() {
        assertRejected("{\"version\":1,\"decks\":[{\"notes\":[]}]}", "no name");
    }

    @Test
    public void noteMissingBackRejected() {
        assertRejected("{\"version\":1,\"decks\":[{\"name\":\"D\","
                + "\"notes\":[{\"front\":\"a\"}]}]}", "missing its front or back");
    }
}

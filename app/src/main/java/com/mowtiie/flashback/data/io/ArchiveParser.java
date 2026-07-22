package com.mowtiie.flashback.data.io;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses and validates archive JSON. Every failure path produces an
 * {@link ArchiveException} whose message can be shown to the user; the parser
 * never returns a partially-built archive.
 *
 * <p>Required fields (deck name, note front and back) are enforced with a
 * message that names the offending index. Optional fields fall back to the same
 * defaults a freshly created entity would use. This is also the validator the
 * Gemini path will feed through in a later phase, so the rules live here rather
 * than in the UI.
 */
public class ArchiveParser {

    private static final int MAX_NAME_LENGTH = 200;

    public DeckArchive parse(String raw) throws ArchiveException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ArchiveException("The file is empty.");
        }

        JSONObject root;
        try {
            root = new JSONObject(raw);
        } catch (JSONException e) {
            throw new ArchiveException(
                    "This does not look like a Flashback file. It is not valid JSON.", e);
        }

        DeckArchive archive = new DeckArchive();

        archive.version = root.optInt("version", -1);
        if (archive.version < 1) {
            throw new ArchiveException(
                    "This file is missing a version number, so it cannot be read as a "
                            + "Flashback export.");
        }
        if (archive.version > DeckArchive.CURRENT_VERSION) {
            throw new ArchiveException(
                    "This file was made by a newer version of Flashback (format "
                            + archive.version + "). Update the app to import it.");
        }

        archive.exportedAt = root.optLong("exportedAt", 0L);

        JSONArray decks = root.optJSONArray("decks");
        if (decks == null) {
            throw new ArchiveException("This file contains no decks to import.");
        }

        try {
            for (int i = 0; i < decks.length(); i++) {
                archive.decks.add(parseDeck(decks.getJSONObject(i), i));
            }
        } catch (JSONException e) {
            throw new ArchiveException("The file's structure is malformed.", e);
        }

        if (archive.decks.isEmpty()) {
            throw new ArchiveException("This file contains no decks to import.");
        }

        return archive;
    }

    private DeckArchive.Deck parseDeck(JSONObject json, int index)
            throws ArchiveException, JSONException {
        DeckArchive.Deck deck = new DeckArchive.Deck();

        deck.name = trimmed(json.optString("name", ""));
        if (deck.name.isEmpty()) {
            throw new ArchiveException("Deck " + (index + 1) + " has no name.");
        }
        if (deck.name.length() > MAX_NAME_LENGTH) {
            deck.name = deck.name.substring(0, MAX_NAME_LENGTH);
        }

        deck.description = optTrimmedOrNull(json, "description");
        deck.reverseByDefault = json.optBoolean("reverseByDefault", false);
        deck.newPerDay = clampPositive(json.optInt("newPerDay", 20), 20);
        deck.reviewsPerDay = clampPositive(json.optInt("reviewsPerDay", 200), 200);

        JSONArray tags = json.optJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.length(); i++) {
                DeckArchive.Tag tag = parseTag(tags.getJSONObject(i));
                if (tag != null) {
                    deck.tags.add(tag);
                }
            }
        }

        JSONArray notes = json.optJSONArray("notes");
        if (notes != null) {
            for (int i = 0; i < notes.length(); i++) {
                deck.notes.add(parseNote(notes.getJSONObject(i), deck.name, i));
            }
        }

        return deck;
    }

    /** A tag with no name is dropped rather than failing the whole import. */
    private DeckArchive.Tag parseTag(JSONObject json) {
        String name = trimmed(json.optString("name", ""));
        if (name.isEmpty()) {
            return null;
        }
        DeckArchive.Tag tag = new DeckArchive.Tag();
        tag.name = name;
        tag.description = optTrimmedOrNull(json, "description");
        tag.color = json.optInt("color", 0);
        return tag;
    }

    private DeckArchive.Note parseNote(JSONObject json, String deckName, int index)
            throws ArchiveException {
        DeckArchive.Note note = new DeckArchive.Note();
        note.front = trimmed(json.optString("front", ""));
        note.back = trimmed(json.optString("back", ""));

        if (note.front.isEmpty() || note.back.isEmpty()) {
            throw new ArchiveException("Card " + (index + 1) + " in deck \""
                    + deckName + "\" is missing its front or back.");
        }
        note.reverseEnabled = json.optBoolean("reverseEnabled", false);
        return note;
    }

    private int clampPositive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private String optTrimmedOrNull(JSONObject json, String key) {
        String value = trimmed(json.optString(key, ""));
        return value.isEmpty() ? null : value;
    }
}

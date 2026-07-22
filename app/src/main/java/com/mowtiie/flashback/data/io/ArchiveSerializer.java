package com.mowtiie.flashback.data.io;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Turns a {@link DeckArchive} into pretty-printed JSON. Pretty printing matters
 * because people open these files, diff them, and hand-edit them.
 */
public class ArchiveSerializer {

    private static final int INDENT = 2;

    public String serialize(DeckArchive archive) {
        try {
            return toJson(archive).toString(INDENT);
        } catch (JSONException e) {
            // The input is fully under our control, so this cannot happen in
            // practice; failing loudly is still better than a silent empty file.
            throw new IllegalStateException("Failed to serialize archive", e);
        }
    }

    private JSONObject toJson(DeckArchive archive) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", archive.version);
        root.put("exportedAt", archive.exportedAt);

        JSONArray decks = new JSONArray();
        for (DeckArchive.Deck deck : archive.decks) {
            decks.put(deckToJson(deck));
        }
        root.put("decks", decks);
        return root;
    }

    private JSONObject deckToJson(DeckArchive.Deck deck) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", deck.name);
        putIfPresent(json, "description", deck.description);
        json.put("reverseByDefault", deck.reverseByDefault);
        json.put("newPerDay", deck.newPerDay);
        json.put("reviewsPerDay", deck.reviewsPerDay);

        JSONArray tags = new JSONArray();
        for (DeckArchive.Tag tag : deck.tags) {
            JSONObject tagJson = new JSONObject();
            tagJson.put("name", tag.name);
            putIfPresent(tagJson, "description", tag.description);
            if (tag.color != 0) {
                tagJson.put("color", tag.color);
            }
            tags.put(tagJson);
        }
        json.put("tags", tags);

        JSONArray notes = new JSONArray();
        for (DeckArchive.Note note : deck.notes) {
            JSONObject noteJson = new JSONObject();
            noteJson.put("front", note.front);
            noteJson.put("back", note.back);
            noteJson.put("reverseEnabled", note.reverseEnabled);
            notes.put(noteJson);
        }
        json.put("notes", notes);
        return json;
    }

    private void putIfPresent(JSONObject json, String key, String value)
            throws JSONException {
        if (value != null && !value.trim().isEmpty()) {
            json.put(key, value);
        }
    }
}

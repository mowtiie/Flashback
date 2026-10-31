package com.mowtiie.flashback.data.io

import org.json.JSONArray
import org.json.JSONObject

class ArchiveSerializer {

    fun serialize(archive: DeckArchive): String = JSONObject().apply {
        put("version", archive.version)
        put("exportedAt", archive.exportedAt)
        put("decks", JSONArray().also { array -> archive.decks.forEach { array.put(deckToJson(it)) } })
    }.toString(INDENT)

    private fun deckToJson(deck: DeckArchive.Deck) = JSONObject().apply {
        put("name", deck.name)
        putIfPresent("description", deck.description)
        put("reverseByDefault", deck.reverseByDefault)
        put("newPerDay", deck.newPerDay)
        put("reviewsPerDay", deck.reviewsPerDay)
        put("tags", JSONArray().also { array -> deck.tags.forEach { array.put(tagToJson(it)) } })
        put("notes", JSONArray().also { array -> deck.notes.forEach { array.put(noteToJson(it)) } })
    }

    private fun tagToJson(tag: DeckArchive.Tag) = JSONObject().apply {
        put("name", tag.name)
        putIfPresent("description", tag.description)
        if (tag.color != 0) put("color", tag.color)
    }

    private fun noteToJson(note: DeckArchive.Note) = JSONObject().apply {
        put("front", note.front)
        put("back", note.back)
        put("reverseEnabled", note.reverseEnabled)
    }

    private fun JSONObject.putIfPresent(key: String, value: String?) {
        if (!value.isNullOrBlank()) put(key, value)
    }

    private companion object {
        const val INDENT = 2
    }
}

package com.mowtiie.flashback.data.io

import org.json.JSONException
import org.json.JSONObject

/** Parses and validates archive JSON. Failures throw [ArchiveException] with a user-readable message. */
class ArchiveParser {

    @Throws(ArchiveException::class)
    fun parse(raw: String?): DeckArchive {
        if (raw.isNullOrBlank()) {
            throw ArchiveException("The file is empty.")
        }

        val root = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw ArchiveException("This does not look like a Flashback file. It is not valid JSON.", e)
        }

        val version = root.optInt("version", -1)
        if (version < 1) {
            throw ArchiveException(
                "This file is missing a version number, so it cannot be read as a Flashback export.",
            )
        }
        if (version > DeckArchive.CURRENT_VERSION) {
            throw ArchiveException(
                "This file was made by a newer version of Flashback (format $version). " +
                    "Update the app to import it.",
            )
        }

        val decksJson = root.optJSONArray("decks")
            ?: throw ArchiveException("This file contains no decks to import.")

        val decks = try {
            List(decksJson.length()) { parseDeck(decksJson.getJSONObject(it), it) }
        } catch (e: JSONException) {
            throw ArchiveException("The file's structure is malformed.", e)
        }
        if (decks.isEmpty()) {
            throw ArchiveException("This file contains no decks to import.")
        }

        return DeckArchive(version, root.optLong("exportedAt", 0L), decks)
    }

    private fun parseDeck(json: JSONObject, index: Int): DeckArchive.Deck {
        val name = json.optString("name", "").trim()
        if (name.isEmpty()) {
            throw ArchiveException("Deck ${index + 1} has no name.")
        }
        val deckName = name.take(MAX_NAME_LENGTH)

        val tags = json.optJSONArray("tags")?.let { array ->
            List(array.length()) { parseTag(array.getJSONObject(it)) }.filterNotNull()
        }.orEmpty()

        val notes = json.optJSONArray("notes")?.let { array ->
            List(array.length()) { parseNote(array.getJSONObject(it), deckName, it) }
        }.orEmpty()

        return DeckArchive.Deck(
            name = deckName,
            description = json.optTrimmedOrNull("description")?.take(MAX_DESCRIPTION_LENGTH),
            reverseByDefault = json.optBoolean("reverseByDefault", false),
            newPerDay = positiveOr(json.optInt("newPerDay", 20), 20),
            reviewsPerDay = positiveOr(json.optInt("reviewsPerDay", 200), 200),
            tags = tags,
            notes = notes,
        )
    }

    private fun parseTag(json: JSONObject): DeckArchive.Tag? {
        val name = json.optString("name", "").trim()
        if (name.isEmpty()) return null

        // 0 means "no colour". Anything else is forced opaque: a stray alpha of
        // zero would otherwise render the chip see-through.
        val color = json.optInt("color", 0)
        return DeckArchive.Tag(
            name = name.take(MAX_TAG_NAME_LENGTH),
            description = json.optTrimmedOrNull("description")?.take(MAX_DESCRIPTION_LENGTH),
            color = if (color == 0) 0 else color or OPAQUE,
        )
    }

    private fun parseNote(json: JSONObject, deckName: String, index: Int): DeckArchive.Note {
        val front = json.optString("front", "").trim()
        val back = json.optString("back", "").trim()

        if (front.isEmpty() || back.isEmpty()) {
            throw ArchiveException(
                "Card ${index + 1} in deck \"$deckName\" is missing its front or back.",
            )
        }
        if (front.length > MAX_CARD_TEXT_LENGTH || back.length > MAX_CARD_TEXT_LENGTH) {
            throw ArchiveException(
                "Card ${index + 1} in deck \"$deckName\" is too long " +
                    "(over $MAX_CARD_TEXT_LENGTH characters).",
            )
        }
        return DeckArchive.Note(front, back, json.optBoolean("reverseEnabled", false))
    }

    private fun positiveOr(value: Int, fallback: Int) = if (value > 0) value else fallback

    private fun JSONObject.optTrimmedOrNull(key: String): String? =
        optString(key, "").trim().ifEmpty { null }

    private companion object {
        const val MAX_NAME_LENGTH = 200
        const val MAX_TAG_NAME_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 1000
        const val MAX_CARD_TEXT_LENGTH = 10_000
        const val OPAQUE = 0xFF000000.toInt()
    }
}

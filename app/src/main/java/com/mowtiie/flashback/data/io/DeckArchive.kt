package com.mowtiie.flashback.data.io

/** In-memory shape of an import/export file. Content only; scheduling state is left out. */
data class DeckArchive(
    /** Bumped only on a breaking change. The parser rejects anything higher. */
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = 0L,
    val decks: List<Deck> = emptyList(),
) {
    data class Deck(
        val name: String,
        val description: String? = null,
        val reverseByDefault: Boolean = false,
        val newPerDay: Int = 20,
        val reviewsPerDay: Int = 200,
        val tags: List<Tag> = emptyList(),
        val notes: List<Note> = emptyList(),
    )

    data class Tag(
        val name: String,
        val description: String? = null,
        val color: Int = 0,
    )

    data class Note(
        val front: String,
        val back: String,
        val reverseEnabled: Boolean = false,
    )

    data class Counts(val decks: Int, val notes: Int, val tags: Int)

    fun counts() = Counts(
        decks = decks.size,
        notes = decks.sumOf { it.notes.size },
        tags = decks.sumOf { it.tags.size },
    )

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** The message is written for the user and shown as is. */
class ArchiveException(message: String, cause: Throwable? = null) : Exception(message, cause)

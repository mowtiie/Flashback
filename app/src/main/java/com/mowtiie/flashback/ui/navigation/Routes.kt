package com.mowtiie.flashback.ui.navigation

object Routes {
    const val DECK_ID = "deckId"
    const val NOTE_ID = "noteId"
    const val TAG_ID = "tagId"

    /** Passed for "create" in the optional id arguments below. */
    const val NEW_ID = -1L

    const val DECKS = "decks"
    const val DECK_DETAIL = "decks/{$DECK_ID}"
    const val DECK_EDITOR = "deck-editor?$DECK_ID={$DECK_ID}"
    const val NOTE_EDITOR = "note-editor/{$DECK_ID}?$NOTE_ID={$NOTE_ID}"
    const val TAG_EDITOR = "tag-editor?$TAG_ID={$TAG_ID}"
    const val STUDY = "study/{$DECK_ID}"

    fun deckDetail(deckId: Long) = "decks/$deckId"

    fun deckEditor(deckId: Long = NEW_ID) = "deck-editor?$DECK_ID=$deckId"

    fun noteEditor(deckId: Long, noteId: Long = NEW_ID) = "note-editor/$deckId?$NOTE_ID=$noteId"

    fun tagEditor(tagId: Long = NEW_ID) = "tag-editor?$TAG_ID=$tagId"

    fun study(deckId: Long) = "study/$deckId"
}

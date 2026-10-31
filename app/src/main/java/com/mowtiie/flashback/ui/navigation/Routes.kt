package com.mowtiie.flashback.ui.navigation

object Routes {
    const val DECK_ID = "deckId"

    const val DECKS = "decks"
    const val DECK_NEW = "decks/new"
    const val DECK_DETAIL = "decks/{$DECK_ID}"
    const val STUDY = "study/{$DECK_ID}"

    fun deckDetail(deckId: Long) = "decks/$deckId"

    fun study(deckId: Long) = "study/$deckId"
}

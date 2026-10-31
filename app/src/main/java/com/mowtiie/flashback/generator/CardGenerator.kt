package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive

/** Turns study material into a deck of cards. Nothing is saved; the caller shows a preview first. */
fun interface CardGenerator {
    @Throws(GeneratorException::class)
    suspend fun generate(source: String): DeckArchive.Deck
}

/** The message is written for the user and shown as is. */
class GeneratorException(message: String, cause: Throwable? = null) : Exception(message, cause)

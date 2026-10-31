package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive

/** Turns study material into a deck of cards. Nothing is saved; the caller shows a preview first. */
fun interface CardGenerator {
    @Throws(GeneratorException::class)
    suspend fun generate(request: GeneratorRequest): DeckArchive.Deck
}

/** The message is written for the user and shown as is. */
class GeneratorException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** What the cards are made from. A PDF is sent whole, so the model reads its layout, tables and scans. */
sealed interface GeneratorInput {
    class Text(val text: String) : GeneratorInput

    class Pdf(val name: String, val bytes: ByteArray) : GeneratorInput
}

/** How many cards to ask for at most. */
enum class CardCount(val max: Int) {
    Few(15),
    Normal(30),
    Many(60),
}

class GeneratorRequest(val input: GeneratorInput, val cardCount: CardCount = CardCount.Normal)

object GeneratorLimits {
    const val MAX_SOURCE_CHARS = 30_000

    /** Gemini accepts about 20 MB per request, and base64 adds a third. */
    const val MAX_PDF_BYTES = 10 * 1024 * 1024
}

/** Each provider needs its own key, which the user supplies. */
enum class GeneratorProvider(val displayName: String) {
    Anthropic("Anthropic"),
    Gemini("Google Gemini"),
}

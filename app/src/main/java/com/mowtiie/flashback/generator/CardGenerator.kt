package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive

/** Turns study material into a deck of cards. Nothing is saved; the caller shows a preview first. */
fun interface CardGenerator {
    @Throws(GeneratorException::class)
    suspend fun generate(request: GeneratorRequest): DeckArchive.Deck
}

/** What a failure means for a run of many requests. */
enum class FailureKind {
    Other,

    /** The key was refused, so sending more requests is pointless. */
    BadKey,

    /** Worth waiting a little and trying again. */
    RateLimited,
}

/** The message is written for the user and shown as is. */
class GeneratorException(
    message: String,
    cause: Throwable? = null,
    val kind: FailureKind = FailureKind.Other,
) : Exception(message, cause)

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

/** How the generated cards should be studied. The user picks one; there is no default in the screen. */
enum class CardTypes {
    /** Plain cards that follow the study setting. */
    Setting,

    /** The model picks flip, type or choice for each card. */
    Mixed,
    Flip,
    Type,
    Choice,
}

/** Everything makes a card for each fact in the material, with no count. Key points keeps a short deck. */
enum class Coverage { Everything, KeyPoints }

class GeneratorRequest(
    val input: GeneratorInput,
    val cardCount: CardCount = CardCount.Normal,
    val cardTypes: CardTypes = CardTypes.Setting,
    val coverage: Coverage = Coverage.KeyPoints,
    /** Fronts of cards already made from earlier sections, so they are not repeated. */
    val known: List<String> = emptyList(),
    /** One section's share of the card count when Key points is split into sections. */
    val cardLimit: Int? = null,
) {
    /** For Everything this is only a safety bound; the model is not asked to stop at it. */
    val maxCards: Int
        get() = cardLimit
            ?: if (coverage == Coverage.Everything) GeneratorLimits.MAX_CARDS_PER_SECTION else cardCount.max
}

object GeneratorLimits {
    /** A sanity ceiling, so a mistake cannot send hundreds of requests on the user's own key. */
    const val MAX_SOURCE_CHARS = 500_000

    /** Key points is one request, so it keeps the old size. */
    const val KEY_POINTS_MAX_CHARS = 30_000

    /** Characters per request when everything is covered. */
    const val SECTION_CHARS = 8_000

    const val MAX_CARDS_PER_SECTION = 300

    /** More requests than this ask the user to confirm first. */
    const val CONFIRM_ABOVE_SECTIONS = 3

    /** Gemini accepts about 20 MB per request, and base64 adds a third. */
    const val MAX_PDF_BYTES = 10 * 1024 * 1024
}

/** Anthropic and Gemini need the user's own key. On-device needs a model file and sends nothing. */
enum class GeneratorProvider(
    val displayName: String,
    val usesNetwork: Boolean = true,
    /** Characters per request when everything is covered. */
    val sectionChars: Int = GeneratorLimits.SECTION_CHARS,
    /** Most text for Key points, which is one request. */
    val keyPointsChars: Int = GeneratorLimits.KEY_POINTS_MAX_CHARS,
) {
    Anthropic("Anthropic"),
    Gemini("Google Gemini"),

    // A small model skips and repeats on long input, so it gets short sections.
    OnDevice("On-device model", usesNetwork = false, sectionChars = 3_000, keyPointsChars = 6_000),
}

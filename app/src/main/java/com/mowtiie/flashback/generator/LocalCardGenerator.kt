package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive

/** A language model that runs on the device. Implementations throw [GeneratorException] with a user-facing message. */
interface TextModel {
    /** [shouldStop] sees the text so far and can end the run early. */
    @Throws(GeneratorException::class)
    suspend fun complete(system: String, user: String, shouldStop: (String) -> Boolean = { false }): String
}

/** Makes cards with a model on the device, so nothing leaves it. Text only; small models cannot read a PDF. */
class LocalCardGenerator(private val model: TextModel) : CardGenerator {

    override suspend fun generate(request: GeneratorRequest): DeckArchive.Deck {
        val text = (request.input as? GeneratorInput.Text)?.text
            ?: throw GeneratorException("The on-device model reads text only. Paste the text, or pick another provider for PDFs.")
        val max = request.maxCards
        val reply = model.complete(CardPrompt.plain(request), CardPrompt.material(text)) { PlainCards.shouldStop(it, max) }
        val parsed = PlainCards.parse(reply, complete = true)
        if (parsed.cards.isEmpty()) {
            throw GeneratorException(
                if (reply.isBlank()) "The model returned no cards." else "The model did not write usable cards. Try a shorter passage.",
            )
        }
        return deckFromModelText(PlainCards.toArchiveJson(parsed, PlainCards.titleFrom(text)), max, request.cardTypes)
    }
}

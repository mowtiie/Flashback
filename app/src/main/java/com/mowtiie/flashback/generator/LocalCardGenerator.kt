package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive

/** A language model that runs on the device. Implementations throw [GeneratorException] with a user-facing message. */
interface TextModel {
    @Throws(GeneratorException::class)
    suspend fun complete(system: String, user: String): String
}

/** Makes cards with a model on the device, so nothing leaves it. Text only; small models cannot read a PDF. */
class LocalCardGenerator(private val model: TextModel) : CardGenerator {

    override suspend fun generate(request: GeneratorRequest): DeckArchive.Deck {
        val text = (request.input as? GeneratorInput.Text)?.text
            ?: throw GeneratorException("The on-device model reads text only. Paste the text, or pick another provider for PDFs.")
        val system = CardPrompt.system(request) + "\n" + CardPrompt.jsonShape(request.cardTypes)
        val reply = model.complete(system, CardPrompt.material(text))
        if (reply.isBlank()) throw GeneratorException("The model returned no cards.")
        return deckFromModelText(reply, request.maxCards, request.cardTypes)
    }
}

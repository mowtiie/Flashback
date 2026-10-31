package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The Messages API request and response for card generation, without any I/O. */
object GeneratorProtocol {
    const val MODEL = "claude-opus-5-5"

    private const val MAX_OUTPUT_TOKENS = 16_000

    private val SCHEMA = JSONObject(
        """
        {
          "type": "object",
          "properties": {
            "version": {"type": "integer"},
            "decks": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": {"type": "string"},
                  "notes": {
                    "type": "array",
                    "items": {
                      "type": "object",
                      "properties": {"front": {"type": "string"}, "back": {"type": "string"}},
                      "required": ["front", "back"],
                      "additionalProperties": false
                    }
                  }
                },
                "required": ["name", "notes"],
                "additionalProperties": false
              }
            }
          },
          "required": ["version", "decks"],
          "additionalProperties": false
        }
        """.trimIndent(),
    )

    fun requestBody(request: GeneratorRequest): String = JSONObject()
        .put("model", MODEL)
        .put("max_tokens", MAX_OUTPUT_TOKENS)
        .put("system", CardPrompt.system(request.cardCount.max))
        .put(
            "output_config",
            JSONObject()
                .put("effort", "low")
                .put("format", JSONObject().put("type", "json_schema").put("schema", SCHEMA)),
        )
        .put(
            "messages",
            JSONArray().put(
                JSONObject().put("role", "user").put("content", userContent(request.input)),
            ),
        )
        .toString()

    /** Text goes in as a string; a PDF as a document block ahead of the instruction. */
    private fun userContent(input: GeneratorInput): Any = when (input) {
        is GeneratorInput.Text -> CardPrompt.material(input.text)
        is GeneratorInput.Pdf -> JSONArray()
            .put(
                JSONObject()
                    .put("type", "document")
                    .put(
                        "source",
                        JSONObject()
                            .put("type", "base64")
                            .put("media_type", "application/pdf")
                            .put("data", CardPrompt.base64(input.bytes)),
                    ),
            )
            .put(JSONObject().put("type", "text").put("text", CardPrompt.PDF_INSTRUCTION))
    }

    /** The model's output goes through [deckFromModelText], so it is validated like any import. */
    @Throws(GeneratorException::class)
    fun parseResponse(status: Int, body: String, maxCards: Int): DeckArchive.Deck {
        if (status !in 200..299) throw GeneratorException(errorMessage(status, body))

        val root = try {
            JSONObject(body)
        } catch (e: JSONException) {
            throw GeneratorException("The reply from Anthropic could not be read.", e)
        }

        when (root.optString("stop_reason")) {
            "refusal" -> throw GeneratorException("The model declined to make cards from this text.")
            "max_tokens" -> throw GeneratorException("The reply was cut off. Try less text at a time.")
        }

        val text = textOf(root.optJSONArray("content"))
        if (text.isEmpty()) throw GeneratorException("The model returned no cards.")

        return deckFromModelText(text, maxCards)
    }

    private fun textOf(content: JSONArray?): String {
        if (content == null) return ""
        return buildString {
            for (i in 0 until content.length()) {
                val block = content.optJSONObject(i) ?: continue
                if (block.optString("type") == "text") append(block.optString("text"))
            }
        }.trim()
    }

    private fun errorMessage(status: Int, body: String): String = when (status) {
        401, 403 -> "Anthropic rejected the API key. Check it in Settings."
        429 -> "Too many requests right now. Wait a minute and try again."
        408, 413 -> "That request was too large. Try less text at a time."
        in 500..599 -> "Anthropic is unavailable at the moment. Try again later."
        else -> apiErrorText(body) ?: "Anthropic returned an error ($status)."
    }

    /** Surfaces messages like a low credit balance, which the user can act on. */
    private fun apiErrorText(body: String): String? = try {
        JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
    } catch (_: JSONException) {
        null
    }
}

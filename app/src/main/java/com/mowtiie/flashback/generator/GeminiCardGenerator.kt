package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive
import kotlinx.coroutines.CoroutineDispatcher
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The Gemini generateContent request and response for card generation, without any I/O. */
object GeminiProtocol {
    const val MODEL = "gemini-3.5-flash-lite"

    private const val MAX_OUTPUT_TOKENS = 16_000

    fun requestBody(request: GeneratorRequest): String = JSONObject()
        .put(
            "systemInstruction",
            textParts("${CardPrompt.system(request.cardCount.max)}\n${CardPrompt.JSON_SHAPE}"),
        )
        .put("contents", JSONArray().put(userContent(request.input)))
        .put(
            "generationConfig",
            JSONObject()
                .put("responseMimeType", "application/json")
                .put("maxOutputTokens", MAX_OUTPUT_TOKENS),
        )
        .toString()

    /** A PDF goes in as inline data ahead of the instruction. */
    private fun userContent(input: GeneratorInput): JSONObject {
        val parts = JSONArray()
        when (input) {
            is GeneratorInput.Text -> parts.put(JSONObject().put("text", CardPrompt.material(input.text)))
            is GeneratorInput.Pdf -> parts
                .put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject().put("mimeType", "application/pdf").put("data", CardPrompt.base64(input.bytes)),
                    ),
                )
                .put(JSONObject().put("text", CardPrompt.PDF_INSTRUCTION))
        }
        return JSONObject().put("role", "user").put("parts", parts)
    }

    private fun textParts(text: String) =
        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))

    @Throws(GeneratorException::class)
    fun parseResponse(status: Int, body: String, maxCards: Int): DeckArchive.Deck {
        if (status !in 200..299) throw GeneratorException(errorMessage(status, body))

        val root = try {
            JSONObject(body)
        } catch (e: JSONException) {
            throw GeneratorException("The reply from Google could not be read.", e)
        }

        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
        if (candidate == null) {
            val blocked = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            throw GeneratorException(
                if (blocked.isEmpty()) "The model returned no cards." else DECLINED,
            )
        }

        when (candidate.optString("finishReason")) {
            "MAX_TOKENS" -> throw GeneratorException("The reply was cut off. Try less text at a time.")
            "SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION" -> throw GeneratorException(DECLINED)
        }

        val text = textOf(candidate.optJSONObject("content")?.optJSONArray("parts"))
        if (text.isEmpty()) throw GeneratorException("The model returned no cards.")

        return deckFromModelText(text, maxCards)
    }

    /** Skips "thought" parts, which hold the model's reasoning, not the answer. */
    private fun textOf(parts: JSONArray?): String {
        if (parts == null) return ""
        return buildString {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (!part.optBoolean("thought", false)) append(part.optString("text"))
            }
        }.trim()
    }

    private fun errorMessage(status: Int, body: String): String {
        val message = apiErrorText(body)
        val badKey = status == 401 || status == 403 ||
            (status == 400 && message?.contains("API key", ignoreCase = true) == true)
        return when {
            badKey -> "Google rejected the API key. Check it in Settings."
            status == 429 -> "The free quota is used up for now. Wait a while and try again."
            status == 404 -> "Google no longer offers the model this app uses. The app needs an update."
            status == 408 || status == 413 -> "That request was too large. Try less text at a time."
            status in 500..599 -> "Google is unavailable at the moment. Try again later."
            else -> message ?: "Google returned an error ($status)."
        }
    }

    private fun apiErrorText(body: String): String? = try {
        JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
    } catch (_: JSONException) {
        null
    }

    private const val DECLINED = "Google declined to make cards from this text."
}

internal object GeminiBackend : GeneratorBackend {
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/${GeminiProtocol.MODEL}:generateContent"

    override val provider = GeneratorProvider.Gemini

    override fun request(request: GeneratorRequest, apiKey: String) = HttpRequest(
        url = ENDPOINT,
        headers = mapOf("content-type" to "application/json", "x-goog-api-key" to apiKey),
        body = GeminiProtocol.requestBody(request),
    )

    override fun parse(status: Int, body: String, maxCards: Int): DeckArchive.Deck =
        GeminiProtocol.parseResponse(status, body, maxCards)
}

/** Calls the Gemini API, which has a free tier. */
class GeminiCardGenerator(
    apiKey: () -> String?,
    ioDispatcher: CoroutineDispatcher,
    transport: HttpTransport = UrlConnectionTransport(),
) : CardGenerator by RemoteCardGenerator(GeminiBackend, apiKey, ioDispatcher, transport)

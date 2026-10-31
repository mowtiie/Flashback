package com.mowtiie.flashback.generator

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Base64

class PdfRequestTest {

    private val encoded = Base64.getEncoder().encodeToString(FAKE_PDF)

    @Test
    fun anthropicSendsThePdfAsADocumentBlockBeforeTheInstruction() {
        val body = JSONObject(GeneratorProtocol.requestBody(pdfRequest(FAKE_PDF)))

        val content = body.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        val document = content.getJSONObject(0)
        assertEquals("document", document.getString("type"))
        assertEquals("application/pdf", document.getJSONObject("source").getString("media_type"))
        assertEquals("base64", document.getJSONObject("source").getString("type"))
        assertEquals(encoded, document.getJSONObject("source").getString("data"))
        assertEquals("text", content.getJSONObject(1).getString("type"))
    }

    @Test
    fun geminiSendsThePdfAsInlineDataBeforeTheInstruction() {
        val body = JSONObject(GeminiProtocol.requestBody(pdfRequest(FAKE_PDF)))

        val user = body.getJSONArray("contents").getJSONObject(0)
        assertEquals("user", user.getString("role"))
        val parts = user.getJSONArray("parts")
        val inline = parts.getJSONObject(0).getJSONObject("inlineData")
        assertEquals("application/pdf", inline.getString("mimeType"))
        assertEquals(encoded, inline.getString("data"))
        assertTrue(parts.getJSONObject(1).getString("text").contains("PDF"))
    }

    @Test
    fun textRequestsAreUnchangedByThePdfSupport() {
        val anthropic = JSONObject(GeneratorProtocol.requestBody(textRequest("notes")))
        assertEquals(
            "<material>\nnotes\n</material>",
            anthropic.getJSONArray("messages").getJSONObject(0).getString("content"),
        )
        assertFalse(GeminiProtocol.requestBody(textRequest("notes")).contains("inlineData"))
    }

    @Test
    fun theCardCountSetsTheLimitInThePrompt() {
        val few = JSONObject(GeneratorProtocol.requestBody(textRequest("x", CardCount.Few))).getString("system")
        val many = JSONObject(GeneratorProtocol.requestBody(textRequest("x", CardCount.Many))).getString("system")
        assertTrue(few.contains("at most 15 cards"))
        assertTrue(many.contains("at most 60 cards"))
    }

    @Test
    fun theCardCountAlsoTrimsALongReply() {
        val notes = (1..40).joinToString(",") { """{"front":"Q$it","back":"A$it"}""" }
        val text = """{"version":1,"decks":[{"name":"D","notes":[$notes]}]}"""
        assertEquals(15, deckFromModelText(text, CardCount.Few.max).notes.size)
        assertEquals(40, deckFromModelText(text, CardCount.Many.max).notes.size)
    }

    @Test
    fun onlyRealPdfsPassTheCheck() {
        assertTrue(looksLikePdf(FAKE_PDF))
        assertFalse(looksLikePdf("PK\u0003\u0004zip".toByteArray()))
        assertFalse(looksLikePdf(ByteArray(0)))
        assertFalse(looksLikePdf("%PDF".toByteArray()))
    }

    @Test
    fun validatedRejectsNonPdfsAndKeepsTheName() {
        assertEquals("a.pdf", PdfInput.validated("a.pdf", FAKE_PDF).name)
        try {
            PdfInput.validated("a.pdf", "not a pdf at all".toByteArray())
            fail("Expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message.orEmpty().contains("does not look like a PDF"))
        }
    }

    @Test
    fun aFileOverTheLimitIsRefusedWhileReading() {
        val big = ByteArray(GeneratorLimits.MAX_PDF_BYTES + 1)
        try {
            PdfInput.readLimited(ByteArrayInputStream(big))
            fail("Expected a GeneratorException")
        } catch (e: GeneratorException) {
            assertTrue(e.message.orEmpty().contains("larger than 10 MB"))
        }
    }

    @Test
    fun aFileAtTheLimitIsRead() {
        val exact = ByteArray(GeneratorLimits.MAX_PDF_BYTES)
        assertEquals(exact.size, PdfInput.readLimited(ByteArrayInputStream(exact)).size)
    }
}

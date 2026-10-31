package com.mowtiie.flashback.data.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchivePastedReplyTest {

    private val json = """{"version":1,"decks":[{"name":"Biology","notes":[{"front":"q","back":"a"}]}]}"""

    private fun names(raw: String) = ArchiveParser().parse(raw).decks.map { it.name }

    @Test
    fun plainJsonStillParses() {
        assertEquals(listOf("Biology"), names(json))
    }

    @Test
    fun aCodeFenceIsIgnored() {
        assertEquals(listOf("Biology"), names("```json\n$json\n```"))
        assertEquals(listOf("Biology"), names("```\n$json\n```"))
    }

    @Test
    fun textBeforeAndAfterTheJsonIsIgnored() {
        assertEquals(listOf("Biology"), names("Sure! Here are your cards:\n\n```json\n$json\n```\n\nLet me know if you want more."))
        assertEquals(listOf("Biology"), names("Here you go: $json Enjoy!"))
    }

    @Test
    fun aByteOrderMarkIsIgnored() {
        assertEquals(listOf("Biology"), names("﻿$json"))
    }

    @Test
    fun bracesInsideCardTextDoNotConfuseIt() {
        val raw = """Result: {"version":1,"decks":[{"name":"Code","notes":[{"front":"What is { }?","back":"A block } {"}]}]} Done."""
        assertEquals(listOf("Code"), names(raw))
        assertEquals("What is { }?", ArchiveParser().parse(raw).decks.single().notes.single().front)
    }

    private fun failsWith(raw: String): String = try {
        ArchiveParser().parse(raw)
        fail("Expected an ArchiveException")
        ""
    } catch (e: ArchiveException) {
        e.message.orEmpty()
    }

    @Test
    fun textWithNoJsonStillFails() {
        assertTrue(failsWith("I could not read that file, sorry.").contains("JSON"))
    }

    @Test
    fun brokenJsonStillFailsWithTheSameMessage() {
        assertTrue(failsWith("""Here: {"version":1,"decks":[{"name":"x",""").contains("JSON"))
    }

    @Test
    fun anEmptyReplyStillFails() {
        assertEquals("The file is empty.", failsWith("   "))
    }
}

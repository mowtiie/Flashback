package com.mowtiie.flashback.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownLiteTest {

    @Test
    fun headingsBulletsAndParagraphsAreSeparated() {
        val blocks = MarkdownLite.parse("# Title\n\nSome text\nmore text\n\n## Part\n\n- one\n- two\n")

        assertEquals(
            listOf(
                MdBlock.Heading(1, listOf(MdSpan("Title"))),
                MdBlock.Paragraph(listOf(MdSpan("Some text more text"))),
                MdBlock.Heading(2, listOf(MdSpan("Part"))),
                MdBlock.Bullet(listOf(MdSpan("one"))),
                MdBlock.Bullet(listOf(MdSpan("two"))),
            ),
            blocks,
        )
    }

    @Test
    fun boldIsMarkedAndBackticksAreDropped() {
        val block = MarkdownLite.parse("- **Internet:** only for `api.example.com`").single()

        assertEquals(listOf(MdSpan("Internet:", bold = true), MdSpan(" only for api.example.com")), block.spans)
    }

    @Test
    fun theBundledPolicyIsTheSameAsPrivacyMd() {
        val bundled = File("src/main/res/raw/privacy_policy.md").readText().replace("\r\n", "\n")
        val source = File("../PRIVACY.md").readText().replace("\r\n", "\n")

        assertEquals("Copy PRIVACY.md to app/src/main/res/raw/privacy_policy.md", source, bundled)
        assertTrue(MarkdownLite.parse(bundled).isNotEmpty())
    }
}

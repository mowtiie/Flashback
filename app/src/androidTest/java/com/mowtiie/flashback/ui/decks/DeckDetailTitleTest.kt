package com.mowtiie.flashback.ui.decks

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.theme.FlashbackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeckDetailTitleTest {

    @get:Rule
    val rule = createComposeRule()

    private val longName = "Cloud computing certification exam preparation notes for the second semester"
    private val twoLineName = "Cloud computing certification exam"

    private fun show(name: String, shortWindow: Boolean) {
        val state = DeckDetailUiState(
            loaded = true,
            deck = Deck(id = 1, name = name),
            notes = listOf(NotePreview(1, "hola", "hello", false)),
        )
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckDetailScreen(state, {}, {}, {}, {}, {}, {}, {}, shortWindow = shortWindow)
            }
        }
    }

    /** Layout results of every text node showing the deck name, in the collapsed and expanded titles. */
    private fun layouts(name: String): List<TextLayoutResult> =
        rule.onAllNodes(hasText(name), useUnmergedTree = true).fetchSemanticsNodes().mapNotNull { node ->
            val out = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(out)
            out.firstOrNull()
        }

    @Test
    fun aLongDeckNameWrapsInsteadOfBeingCutOffOnANormalWindow() {
        show(twoLineName, shortWindow = false)
        val wrapped = layouts(twoLineName).any { it.lineCount >= 2 && !it.hasVisualOverflow }
        assertTrue("expected a title that wraps to two lines without being cut off", wrapped)
    }

    @Test
    fun aShortWindowKeepsTheSingleLineBar() {
        show(longName, shortWindow = true)
        val results = layouts(longName)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.lineCount == 1 })
    }

    @Test
    fun aShortNameStaysOnOneLine() {
        show("Spanish", shortWindow = false)
        val results = layouts("Spanish")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.lineCount == 1 })
    }
}

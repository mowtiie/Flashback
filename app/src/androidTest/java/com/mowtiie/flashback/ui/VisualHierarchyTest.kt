package com.mowtiie.flashback.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.SectionTitle
import com.mowtiie.flashback.ui.components.TagChip
import com.mowtiie.flashback.ui.decks.DeckListScreen
import com.mowtiie.flashback.ui.decks.previewDeckListState
import com.mowtiie.flashback.ui.settings.ChoiceRow
import com.mowtiie.flashback.ui.settings.ChoiceRowStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualHierarchyTest {

    @get:Rule
    val rule = createComposeRule()

    private fun styleOf(node: SemanticsNodeInteraction): TextStyle {
        val results = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().layoutInput.style
    }

    private fun androidx.compose.ui.unit.DpRect.span() = right - left

    // 1. The deck name outranks the counters.
    @Test
    fun theDeckNameIsLargerThanTheCounters() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                DeckListScreen(
                    state = previewDeckListState(),
                    onSelectTag = {}, onOpenDeck = {}, onStudyDeck = {}, onAddDeck = {},
                    onGenerate = {}, onPromptHelper = {}, shortWindow = false,
                )
            }
        }
        val name = styleOf(rule.onNodeWithText("Spanish vocabulary")).fontSize.value
        val count = styleOf(rule.onNodeWithText("47")).fontSize.value
        assertTrue("name $name should be larger than count $count", name > count)
    }

    // 2. Chips: a long name is capped, and a neutral tag renders like any other.
    private fun chip(tag: Tag) = rule.setContent { FlashbackTheme(dynamicColor = false) { TagChip(tag) } }

    @Test
    fun aLongTagNameIsCappedInsteadOfStretchingTheChip() {
        chip(Tag(name = "Cloud computing certification exam and more words to be sure", color = 0))
        val width = rule.onNodeWithText("Cloud computing", substring = true).getBoundsInRoot().span()
        assertTrue("chip text is $width wide", width <= 160.dp)
    }

    @Test
    fun aShortTagNameKeepsItsNaturalWidth() {
        chip(Tag(name = "Biology", color = 0))
        assertTrue(rule.onNodeWithText("Biology").getBoundsInRoot().span() < 120.dp)
    }

    // 3. Choice rows line up with the other fields of a form.
    private fun formWithChoice(style: ChoiceRowStyle) {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(value = "field", onValueChange = {}, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                    ChoiceRow(
                        title = "Mode",
                        options = listOf("A", "B"),
                        selected = "A",
                        label = { it },
                        onSelect = {},
                        style = style,
                    )
                }
            }
        }
    }

    @Test
    fun aFormChoiceRowStartsAtTheSameEdgeAsTheFields() {
        formWithChoice(ChoiceRowStyle.Form)
        val field = rule.onNodeWithText("field").getBoundsInRoot().left
        val title = rule.onNodeWithText("Mode").getBoundsInRoot().left
        // The field node is its whole box, so the title lines up with the box edge.
        assertEquals(field, title)
    }

    @Test
    fun aCardChoiceRowKeepsItsInsetInsideACard() {
        formWithChoice(ChoiceRowStyle.Card)
        val form = rule.onNodeWithText("Mode").getBoundsInRoot().left
        assertEquals(32.dp, form)
    }

    // 4. Question labels in forms share one style.
    @Test
    fun aFormChoiceRowTitleUsesTheQuestionLabelStyle() {
        rule.setContent {
            FlashbackTheme(dynamicColor = false) {
                Column {
                    ChoiceRow(title = "Mode", options = listOf("A", "B"), selected = "A", label = { it }, onSelect = {}, style = ChoiceRowStyle.Form)
                    Text("Question", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                }
            }
        }
        val form = styleOf(rule.onNodeWithText("Mode"))
        val reference = styleOf(rule.onNodeWithText("Question"))
        assertEquals(reference.fontSize, form.fontSize)
        assertEquals(reference.fontWeight, form.fontWeight)
    }

    @Test
    fun aCardChoiceRowTitleKeepsTheRowTitleStyle() {
        formWithChoice(ChoiceRowStyle.Card)
        val card = styleOf(rule.onNodeWithText("Mode"))
        assertTrue(card.fontSize.value >= 16f)
    }

    // 5. Section titles share one style and are headings.
    @Test
    fun sectionTitlesAreHeadingsInTheAccentStyle() {
        rule.setContent { FlashbackTheme(dynamicColor = false) { SectionTitle("Activity") } }
        val node = rule.onNodeWithText("Activity")
        node.assert(SemanticsMatcher("is a heading") { it.config.getOrNull(SemanticsProperties.Heading) != null })
        val style = styleOf(node)
        assertEquals(androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp), style.fontSize)
    }
}

package com.mowtiie.flashback.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.mowtiie.flashback.R
import com.mowtiie.flashback.data.model.DeckListItem
import com.mowtiie.flashback.theme.countColors
import com.mowtiie.flashback.ui.components.TagChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckCard(
    item: DeckListItem,
    onOpen: () -> Unit,
    onStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = item.summary
    val deck = summary.deck
    val colors = MaterialTheme.countColors

    OutlinedCard(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (!deck.description.isNullOrBlank()) {
                Text(
                    text = deck.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (item.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item.tags.forEach { TagChip(it) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                CountBlock(summary.availableNew(), R.string.count_new, colors.new)
                CountBlock(summary.learnCount, R.string.count_learning, colors.learning)
                CountBlock(summary.availableDue(), R.string.count_due, colors.due)
                Text(
                    text = pluralStringResource(
                        R.plurals.deck_card_count,
                        summary.totalCount,
                        summary.totalCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val studyable = summary.studyableCount()
            if (studyable > 0) {
                FilledTonalButton(
                    onClick = onStudy,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.deck_study_button, studyable))
                }
            }
        }
    }
}

/** Zero reads as quiet: it falls back to the neutral label colour. */
@Composable
private fun RowScope.CountBlock(count: Int, labelRes: Int, activeColor: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(labelRes).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.08.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

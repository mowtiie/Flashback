package com.mowtiie.flashback.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.theme.countColors
import com.mowtiie.flashback.ui.components.CenteredScrollColumn
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.tags.TagPalette
import java.util.Locale

@Composable
fun StatisticsRoute(viewModel: StatisticsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    StatisticsScreen(state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(state: StatisticsUiState) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            !state.loaded -> Box(Modifier.fillMaxSize().padding(padding))

            !state.data.hasAnyActivity -> EmptyState(
                title = stringResource(R.string.stats_empty_title),
                body = stringResource(R.string.stats_empty_body),
                icon = R.drawable.ic_stats,
                modifier = Modifier.padding(padding),
            )

            else -> CenteredScrollColumn(Modifier.padding(padding)) { StatisticsContent(state.data) }
        }
    }
}

@Composable
private fun ColumnScope.StatisticsContent(data: StatisticsData) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatTile(
            value = data.reviewsToday.toString(),
            label = stringResource(R.string.stats_reviews_today),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = pluralStringResource(R.plurals.stats_streak_value, data.streakDays, data.streakDays),
            label = stringResource(R.string.stats_streak),
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StatTile(
            value = data.retentionPercent
                ?.let { stringResource(R.string.stats_percent, it) }
                ?: stringResource(R.string.stats_no_value),
            label = stringResource(R.string.stats_retention),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = data.timeToday.text(),
            label = stringResource(R.string.stats_time_today),
            modifier = Modifier.weight(1f),
        )
    }

    SectionTitle(R.string.stats_activity_title)
    ActivityCard(data.days)

    SectionTitle(R.string.stats_breakdown_title)
    BreakdownCard(data.breakdown)
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun ActivityCard(days: List<DayBar>) {
    val description = stringResource(
        R.string.stats_chart_desc,
        days.size,
        days.joinToString(", ") { it.count.toString() },
    )
    OutlinedCard(Modifier.fillMaxWidth()) {
        ActivityChart(days, description, Modifier.padding(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BreakdownCard(breakdown: CardStateBreakdown) {
    val colors = MaterialTheme.countColors
    val entries = listOf(
        LegendEntry(stringResource(R.string.count_new), breakdown.newCount, colors.new),
        LegendEntry(stringResource(R.string.count_learning), breakdown.learningCount, colors.learning),
        LegendEntry(stringResource(R.string.stats_young), breakdown.youngCount, Color(TagPalette.colors[TEAL_INDEX])),
        LegendEntry(stringResource(R.string.stats_mature), breakdown.matureCount, colors.due),
    )
    val legendText = entries.map { stringResource(R.string.stats_legend_entry, it.label, it.count) }
    val description = stringResource(R.string.stats_breakdown_desc, legendText.joinToString(", "))

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SegmentBar(entries.map { it.count }, entries.map { it.color }, description)
            Text(
                text = pluralStringResource(R.plurals.stats_total_cards, breakdown.total(), breakdown.total()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                entries.forEach { LegendItem(it) }
            }
        }
    }
}

private const val TEAL_INDEX = 5

private class LegendEntry(val label: String, val count: Int, val color: Color)

@Composable
private fun LegendItem(entry: LegendEntry) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(entry.color, CircleShape))
        Text(
            text = stringResource(R.string.stats_legend_entry, entry.label, entry.count),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun TimeSpent.text(): String = when (this) {
    is TimeSpent.Minutes -> stringResource(R.string.stats_minutes, value)
    is TimeSpent.Hours ->
        stringResource(R.string.stats_hours, String.format(Locale.getDefault(), "%.1f", value))
}

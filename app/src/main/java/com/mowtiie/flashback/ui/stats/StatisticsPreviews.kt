package com.mowtiie.flashback.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.LightDarkPreviews

private val previewCounts = listOf(0, 4, 12, 30, 22, 0, 9, 41, 25, 18, 0, 7, 33, 15)

private val previewData = StatisticsData(
    reviewsToday = 15,
    reviewsThisWeek = 131,
    totalReviews = 1204,
    streakDays = 12,
    retentionPercent = 87,
    timeToday = TimeSpent.Minutes(23),
    breakdown = CardStateBreakdown(newCount = 48, learningCount = 9, youngCount = 71, matureCount = 84),
    days = previewCounts.mapIndexed { index, count ->
        DayBar(count, if (index == 0 || index == 13 || (13 - index) % 3 == 0) "${index + 8}" else "")
    },
)

@Composable
private fun Preview(state: StatisticsUiState) {
    FlashbackTheme(dynamicColor = false) { StatisticsScreen(state) }
}

@LightDarkPreviews
@Composable
private fun StatisticsPreview() = Preview(StatisticsUiState(loaded = true, data = previewData))

@LightDarkPreviews
@Composable
private fun StatisticsLongTimeNoRetentionPreview() = Preview(
    StatisticsUiState(
        loaded = true,
        data = previewData.copy(
            retentionPercent = null,
            timeToday = TimeSpent.Hours(1.5),
            streakDays = 0,
            days = previewData.days.map { it.copy(count = 0) },
        ),
    ),
)

@LightDarkPreviews
@Composable
private fun StatisticsEmptyPreview() = Preview(StatisticsUiState(loaded = true))

@Preview(name = "Wide", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun StatisticsWidePreview() = Preview(StatisticsUiState(loaded = true, data = previewData))

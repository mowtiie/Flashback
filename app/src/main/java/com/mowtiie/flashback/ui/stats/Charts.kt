package com.mowtiie.flashback.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val BAR_WIDTH_FRACTION = 0.6f

/** Bars with a track behind each; the project carries no charting library. */
@Composable
fun ActivityChart(days: List<DayBar>, description: String, modifier: Modifier = Modifier) {
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = description },
    ) {
        if (days.isEmpty()) return@Canvas

        val labelGap = 6.dp.toPx()
        val labelHeight = measurer.measure("0", labelStyle).size.height + labelGap
        val chartHeight = size.height - labelHeight
        val slot = size.width / days.size
        val barWidth = slot * BAR_WIDTH_FRACTION
        val corner = CornerRadius(3.dp.toPx())
        val minBar = 2.dp.toPx()
        val peak = max(1, days.maxOf { it.count })

        days.forEachIndexed { index, day ->
            val left = slot * index + (slot - barWidth) / 2f
            drawRoundRect(track, Offset(left, 0f), Size(barWidth, chartHeight), corner)

            if (day.count > 0) {
                val height = max(minBar, chartHeight * day.count / peak)
                drawRoundRect(
                    bar,
                    Offset(left, chartHeight - height),
                    Size(barWidth, height),
                    corner,
                )
            }

            if (day.label.isNotEmpty()) {
                val text = measurer.measure(day.label, labelStyle)
                drawText(
                    text,
                    topLeft = Offset(
                        slot * index + (slot - text.size.width) / 2f,
                        chartHeight + labelGap / 2f,
                    ),
                )
            }
        }
    }
}

@Composable
fun SegmentBar(
    values: List<Int>,
    colors: List<Color>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val empty = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val total = values.sum()
        val corner = CornerRadius(6.dp.toPx())
        if (total == 0) {
            drawRoundRect(empty, size = size, cornerRadius = corner)
            return@Canvas
        }

        // Round the whole bar by clipping segments to it.
        val path = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner))
        }
        clipPath(path) {
            var x = 0f
            values.forEachIndexed { index, value ->
                if (value == 0) return@forEachIndexed
                val width = size.width * value / total
                drawRect(colors[index], Offset(x, 0f), Size(width, size.height))
                x += width
            }
        }
    }
}

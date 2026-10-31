package com.mowtiie.flashback.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mowtiie.flashback.R

/** Long text steps down so a paragraph stays on the card. */
internal fun cardTextSize(text: String): TextUnit = when {
    text.length <= 30 -> 34.sp
    text.length <= 80 -> 28.sp
    text.length <= 200 -> 24.sp
    else -> 20.sp
}

/**
 * [rotationY] drives the flip. [onReveal] is non-null while the answer is hidden in flip mode,
 * which makes the whole card tappable as well as the Show answer button.
 */
@Composable
internal fun StudyCardFace(
    card: StudyCardUi,
    revealed: Boolean,
    rotationY: Float,
    onReveal: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    // A new card starts at the top, not wherever the last long one was scrolled to.
    val scroll = remember(card) { androidx.compose.foundation.ScrollState(0) }

    Card(
        modifier = modifier.graphicsLayer {
            this.rotationY = rotationY
            cameraDistance = 8000f * density
        },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cardHeight = maxHeight
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .heightIn(min = cardHeight)
                    .then(
                        if (onReveal != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = stringResource(R.string.study_show_answer),
                                onClick = onReveal,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(24.dp),
            ) {
                Text(
                    text = card.question,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = cardTextSize(card.question),
                        lineHeight = cardTextSize(card.question) * 1.25f,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (revealed) {
                    Spacer(Modifier.height(24.dp))
                    Spacer(
                        Modifier
                            .width(64.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = card.answer,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = cardTextSize(card.answer),
                            lineHeight = cardTextSize(card.answer) * 1.25f,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

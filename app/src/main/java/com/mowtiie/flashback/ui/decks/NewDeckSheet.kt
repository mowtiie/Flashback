package com.mowtiie.flashback.ui.decks

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.components.asHeading
import com.mowtiie.flashback.ui.settings.ActionRow
import kotlinx.coroutines.launch

internal const val NEW_DECK_SHEET_TAG = "new-deck-sheet"

/** The ways to start a deck, in a bottom sheet like the import one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewDeckSheet(
    onManual: () -> Unit,
    onGenerate: () -> Unit,
    onPromptHelper: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Slide the sheet away first, then navigate, so the next screen does not open over a sheet.
    fun choose(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(NEW_DECK_SHEET_TAG),
    ) {
        Text(
            text = stringResource(R.string.new_deck_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).asHeading(),
        )
        ActionRow(
            title = stringResource(R.string.new_deck_manual),
            body = stringResource(R.string.new_deck_manual_body),
            leadingIcon = R.drawable.ic_edit,
            onClick = { choose(onManual) },
        )
        ActionRow(
            title = stringResource(R.string.new_deck_ai),
            body = stringResource(R.string.new_deck_ai_body),
            leadingIcon = R.drawable.ic_generate,
            onClick = { choose(onGenerate) },
        )
        ActionRow(
            title = stringResource(R.string.new_deck_hybrid),
            body = stringResource(R.string.new_deck_hybrid_body),
            leadingIcon = R.drawable.ic_paste,
            onClick = { choose(onPromptHelper) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

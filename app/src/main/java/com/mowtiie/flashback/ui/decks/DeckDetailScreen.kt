package com.mowtiie.flashback.ui.decks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mowtiie.flashback.ui.components.ActionFab
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.settings.RowDivider
import com.mowtiie.flashback.ui.settings.ActionRow
import com.mowtiie.flashback.ui.components.rememberFabExpanded
import androidx.compose.foundation.lazy.rememberLazyListState
import com.mowtiie.flashback.ui.components.isShortWindow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.MediumTopAppBar
import com.mowtiie.flashback.ui.components.TagChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.mowtiie.flashback.ui.study.AnswerMode
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.io.deckExportFileName
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.ui.export.rememberSaveDocumentLauncher

@Composable
fun DeckDetailRoute(
    viewModel: DeckDetailViewModel,
    onBack: () -> Unit,
    onEditDeck: () -> Unit,
    onAddNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onStudy: () -> Unit,
    onDeleted: (name: String, undo: FlashbackRepository.UndoAction) -> Unit,
    onMessage: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val saveLauncher = rememberSaveDocumentLauncher(
        onChosen = viewModel::saveExport,
        onCancelled = viewModel::discardExport,
    )

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DeckDetailEvent.Deleted -> onDeleted(event.deckName, event.undo)
                is DeckDetailEvent.ExportReady -> saveLauncher.launch(
                    deckExportFileName(resources.getString(R.string.export_deck_filename_prefix), event.deckName),
                )
                DeckDetailEvent.ExportNothing -> onMessage(R.string.export_deck_missing)
                DeckDetailEvent.ExportSaved -> onMessage(R.string.export_saved)
                DeckDetailEvent.ExportWriteFailed -> onMessage(R.string.export_write_failed)
            }
        }
    }

    DeckDetailScreen(
        state = state,
        onBack = onBack,
        onEditDeck = onEditDeck,
        onExportDeck = viewModel::exportDeck,
        onDeleteDeck = viewModel::deleteDeck,
        onAddNote = onAddNote,
        onOpenNote = onOpenNote,
        onStudy = onStudy,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    state: DeckDetailUiState,
    onBack: () -> Unit,
    onEditDeck: () -> Unit,
    onExportDeck: () -> Unit,
    onDeleteDeck: () -> Unit,
    onAddNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onStudy: () -> Unit,
    shortWindow: Boolean = isShortWindow(),
) {
    // A long deck name wraps in the medium bar; a short window keeps the one-line bar to save height.
    val scrollBehavior = if (shortWindow) {
        TopAppBarDefaults.pinnedScrollBehavior()
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    }

    val listState = rememberLazyListState()
    val fabExpanded = rememberFabExpanded(listState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val title: @Composable () -> Unit = {
                Text(
                    text = state.deck?.name.orEmpty(),
                    fontFamily = FontFamily.Serif,
                    maxLines = if (shortWindow) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val navigationIcon: @Composable () -> Unit = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_up),
                    )
                }
            }
            val actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
                DeckDetailActions(onAddNote, onEditDeck)
            }
            if (shortWindow) {
                TopAppBar(title = title, navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior)
            } else {
                MediumTopAppBar(title = title, navigationIcon = navigationIcon, actions = actions, scrollBehavior = scrollBehavior)
            }
        },
        floatingActionButton = {
            if (state.studyableCount > 0) {
                ActionFab(
                    label = stringResource(R.string.deck_study_button, state.studyableCount),
                    onClick = onStudy,
                    icon = { Icon(painterResource(R.drawable.ic_school), contentDescription = null) },
                    expanded = fabExpanded,
                )
            }
        },
    ) { padding ->
        if (state.loaded && state.notes.isEmpty()) {
            Column(Modifier.padding(padding).padding(16.dp)) {
                EmptyState(
                    title = stringResource(R.string.deck_detail_empty_title),
                    body = stringResource(R.string.deck_detail_empty_body),
                    actionLabel = stringResource(R.string.deck_detail_empty_action),
                    onAction = onAddNote,
                    modifier = Modifier.weight(1f),
                )
                DeckManagement(onExportDeck, onDeleteDeck)
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "summary") { DeckSummary(state) }
                items(state.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        suspended = note.id in state.suspendedNoteIds,
                        onClick = { onOpenNote(note.id) },
                    )
                }
                item(key = "manage") { DeckManagement(onExportDeck, onDeleteDeck, Modifier.padding(top = 16.dp)) }
            }
        }
    }
}

/** Two actions at most in the bar; export and delete sit at the end of the page instead of an overflow menu. */
@Composable
private fun DeckDetailActions(onAddNote: () -> Unit, onEditDeck: () -> Unit) {
    IconButton(onClick = onAddNote) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.note_add))
    }
    IconButton(onClick = onEditDeck) {
        Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.deck_editor_edit_title))
    }
}

@Composable
private fun DeckManagement(onExport: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            ActionRow(
                title = stringResource(R.string.deck_export),
                onClick = onExport,
                leadingIcon = R.drawable.ic_download,
            )
            RowDivider()
            ActionRow(
                title = stringResource(R.string.deck_delete),
                onClick = onDelete,
                leadingIcon = R.drawable.ic_delete,
                destructive = true,
            )
        }
    }
}

/** Description, tags and today's counts, so the page says what the deck is before listing its cards. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeckSummary(state: DeckDetailUiState) {
    val description = state.deck?.description
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
    ) {
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.tags.forEach { TagChip(it) }
            }
        }
        Text(
            text = stringResource(R.string.deck_detail_counts, state.newCount, state.learnCount, state.dueCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NoteCard(note: NotePreview, suspended: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (suspended) 0.6f else 1f),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = note.front,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = note.back,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            note.answerMode?.let {
                Text(
                    text = stringResource(AnswerMode.fromValue(it).labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (note.reverseEnabled) {
                TwoWayBadge(Modifier.padding(top = 8.dp))
            }
            if (suspended) {
                Text(
                    text = stringResource(R.string.note_suspended_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** A badge, not a button: it has no click behaviour to announce. */
@Composable
private fun TwoWayBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_swap),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.note_has_reverse),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun AnswerMode.labelRes(): Int = when (this) {
    AnswerMode.Flip -> R.string.answer_mode_flip
    AnswerMode.Type -> R.string.answer_mode_type
    AnswerMode.Choice -> R.string.answer_mode_choice
}

private val previewNotes = listOf(
    NotePreview(1, "el bosque", "the forest", reverseEnabled = true),
    NotePreview(2, "la casa", "the house", reverseEnabled = false, answerMode = AnswerMode.Type.value),
    NotePreview(3, "un ejemplo muy largo ".repeat(6), "a very long back ".repeat(6), false),
)

@LightDarkPreviews
@Composable
private fun DeckDetailPreview() {
    FlashbackTheme(dynamicColor = false) {
        DeckDetailScreen(
            state = DeckDetailUiState(
                loaded = true,
                deck = Deck(id = 1, name = "Spanish vocabulary"),
                notes = previewNotes,
                suspendedNoteIds = setOf(2L),
                studyableCount = 14,
            ),
            onBack = {}, onEditDeck = {}, onExportDeck = {}, onDeleteDeck = {}, onAddNote = {},
            onOpenNote = {}, onStudy = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun DeckDetailEmptyPreview() {
    FlashbackTheme(dynamicColor = false) {
        DeckDetailScreen(
            state = DeckDetailUiState(loaded = true, deck = Deck(id = 1, name = "Spanish vocabulary")),
            onBack = {}, onEditDeck = {}, onExportDeck = {}, onDeleteDeck = {}, onAddNote = {},
            onOpenNote = {}, onStudy = {},
        )
    }
}

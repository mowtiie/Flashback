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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.model.NotePreview
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.components.LightDarkPreviews

@Composable
fun DeckDetailRoute(
    viewModel: DeckDetailViewModel,
    onBack: () -> Unit,
    onEditDeck: () -> Unit,
    onAddNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onStudy: () -> Unit,
    onDeleted: (name: String, undo: FlashbackRepository.UndoAction) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is DeckDetailEvent.Deleted -> onDeleted(event.deckName, event.undo)
            }
        }
    }

    DeckDetailScreen(
        state = state,
        onBack = onBack,
        onEditDeck = onEditDeck,
        onDeleteDeck = { confirmDelete = true },
        onAddNote = onAddNote,
        onOpenNote = onOpenNote,
        onStudy = onStudy,
    )

    val deckName = state.deck?.name
    if (confirmDelete && deckName != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.deck_delete_title)) },
            text = { Text(stringResource(R.string.deck_delete_body, deckName)) },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteDeck()
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    state: DeckDetailUiState,
    onBack: () -> Unit,
    onEditDeck: () -> Unit,
    onDeleteDeck: () -> Unit,
    onAddNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onStudy: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.deck?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                actions = { DeckDetailActions(onAddNote, onEditDeck, onDeleteDeck) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (state.studyableCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onStudy,
                    icon = {
                        Icon(painterResource(R.drawable.ic_school), contentDescription = null)
                    },
                    text = { Text(stringResource(R.string.deck_study_button, state.studyableCount)) },
                )
            }
        },
    ) { padding ->
        if (state.loaded && state.notes.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.deck_detail_empty_title),
                body = stringResource(R.string.deck_detail_empty_body),
                actionLabel = stringResource(R.string.deck_detail_empty_action),
                onAction = onAddNote,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        suspended = note.id in state.suspendedNoteIds,
                        onClick = { onOpenNote(note.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckDetailActions(onAddNote: () -> Unit, onEditDeck: () -> Unit, onDeleteDeck: () -> Unit) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = onAddNote) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.note_add))
    }
    IconButton(onClick = { menuOpen = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.deck_editor_edit_title)) },
            onClick = {
                menuOpen = false
                onEditDeck()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.deck_delete)) },
            onClick = {
                menuOpen = false
                onDeleteDeck()
            },
        )
    }
}

@Composable
private fun NoteCard(note: NotePreview, suspended: Boolean, onClick: () -> Unit) {
    OutlinedCard(
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

private val previewNotes = listOf(
    NotePreview(1, "el bosque", "the forest", reverseEnabled = true),
    NotePreview(2, "la casa", "the house", reverseEnabled = false),
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
            onBack = {}, onEditDeck = {}, onDeleteDeck = {}, onAddNote = {},
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
            onBack = {}, onEditDeck = {}, onDeleteDeck = {}, onAddNote = {},
            onOpenNote = {}, onStudy = {},
        )
    }
}

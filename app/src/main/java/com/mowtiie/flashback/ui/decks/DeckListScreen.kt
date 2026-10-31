package com.mowtiie.flashback.ui.decks

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.components.LightDarkPreviews

@Composable
fun DeckListRoute(
    viewModel: DeckListViewModel,
    onOpenDeck: (Long) -> Unit,
    onStudyDeck: (Long) -> Unit,
    onAddDeck: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    DeckListScreen(
        state = state,
        onSelectTag = viewModel::selectTag,
        onOpenDeck = onOpenDeck,
        onStudyDeck = onStudyDeck,
        onAddDeck = onAddDeck,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    state: DeckListUiState,
    onSelectTag: (Long?) -> Unit,
    onOpenDeck: (Long) -> Unit,
    onStudyDeck: (Long) -> Unit,
    onAddDeck: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val empty = state.loaded && state.items.isEmpty()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.decks_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (state.items.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddDeck,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.deck_add)) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.tags.isNotEmpty()) {
                TagFilterRow(state, onSelectTag)
            }
            if (empty) {
                DeckListEmpty(state.isFiltered, onAddDeck)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.summary.deck.id }) { item ->
                        DeckCard(
                            item = item,
                            onOpen = { onOpenDeck(item.summary.deck.id) },
                            onStudy = { onStudyDeck(item.summary.deck.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagFilterRow(state: DeckListUiState, onSelectTag: (Long?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            TagFilterChip(
                label = stringResource(R.string.filter_all),
                selected = state.selectedTagId == null,
                onClick = { onSelectTag(null) },
            )
        }
        items(state.tags, key = { it.id }) { tag ->
            TagFilterChip(
                label = tag.name,
                selected = state.selectedTagId == tag.id,
                onClick = { onSelectTag(tag.id) },
            )
        }
    }
}

@Composable
private fun TagFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun DeckListEmpty(filtered: Boolean, onAddDeck: () -> Unit) {
    if (filtered) {
        EmptyState(
            title = stringResource(R.string.decks_filtered_empty_title),
            body = stringResource(R.string.decks_filtered_empty_body),
        )
    } else {
        EmptyState(
            title = stringResource(R.string.decks_empty_title),
            body = stringResource(R.string.decks_empty_body),
            actionLabel = stringResource(R.string.decks_empty_action),
            onAction = onAddDeck,
        )
    }
}

@LightDarkPreviews
@Composable
private fun DeckListPreview() {
    FlashbackTheme(dynamicColor = false) {
        DeckListScreen(
            state = previewDeckListState(),
            onSelectTag = {},
            onOpenDeck = {},
            onStudyDeck = {},
            onAddDeck = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun DeckListEmptyPreview() {
    FlashbackTheme(dynamicColor = false) {
        DeckListScreen(
            state = DeckListUiState(loaded = true),
            onSelectTag = {},
            onOpenDeck = {},
            onStudyDeck = {},
            onAddDeck = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun DeckListFilteredEmptyPreview() {
    val tags = previewDeckListState().tags
    FlashbackTheme(dynamicColor = false) {
        DeckListScreen(
            state = DeckListUiState(loaded = true, tags = tags, selectedTagId = tags.first().id),
            onSelectTag = {},
            onOpenDeck = {},
            onStudyDeck = {},
            onAddDeck = {},
        )
    }
}

package com.mowtiie.flashback.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mowtiie.flashback.R
import com.mowtiie.flashback.data.entity.Tag
import com.mowtiie.flashback.data.model.TagWithCount
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.ui.components.EmptyState
import com.mowtiie.flashback.ui.components.LightDarkPreviews

@Composable
fun TagsRoute(viewModel: TagsViewModel, onOpenTag: (Long) -> Unit, onAddTag: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TagsScreen(state = state, onOpenTag = onOpenTag, onAddTag = onAddTag)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(state: TagsUiState, onOpenTag: (Long) -> Unit, onAddTag: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (state.tags.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddTag,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.tag_add)) },
                )
            }
        },
    ) { padding ->
        if (state.loaded && state.tags.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.tags_empty_title),
                body = stringResource(R.string.tags_empty_body),
                icon = R.drawable.ic_tags,
                actionLabel = stringResource(R.string.tag_add),
                onAction = onAddTag,
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
                items(state.tags, key = { it.tag.id }) { item ->
                    TagCard(item, onClick = { onOpenTag(item.tag.id) })
                }
            }
        }
    }
}

@Composable
private fun TagCard(item: TagWithCount, onClick: () -> Unit) {
    val tag = item.tag
    val swatch = if (tag.color == 0) MaterialTheme.colorScheme.outline else Color(tag.color)

    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(Modifier.size(24.dp).background(swatch, CircleShape))
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!tag.description.isNullOrBlank()) {
                    Text(
                        text = tag.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (item.deckCount == 0) {
                        stringResource(R.string.tag_unused)
                    } else {
                        pluralStringResource(R.plurals.tag_deck_count, item.deckCount, item.deckCount)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private val previewTags = listOf(
    TagWithCount(Tag(1, "Biology", "Year 2 modules", TagPalette.colors[4]), 3),
    TagWithCount(Tag(2, "Languages", null, TagPalette.colors[7]), 1),
    TagWithCount(Tag(3, "Unsorted", null, 0), 0),
)

@LightDarkPreviews
@Composable
private fun TagsPreview() {
    FlashbackTheme(dynamicColor = false) {
        TagsScreen(TagsUiState(loaded = true, tags = previewTags), onOpenTag = {}, onAddTag = {})
    }
}

@LightDarkPreviews
@Composable
private fun TagsEmptyPreview() {
    FlashbackTheme(dynamicColor = false) {
        TagsScreen(TagsUiState(loaded = true), onOpenTag = {}, onAddTag = {})
    }
}

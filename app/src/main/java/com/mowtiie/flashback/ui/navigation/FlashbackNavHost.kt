package com.mowtiie.flashback.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mowtiie.flashback.AppContainer
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.decks.DeckDetailRoute
import com.mowtiie.flashback.ui.decks.DeckDetailViewModel
import com.mowtiie.flashback.ui.decks.DeckEditorRoute
import com.mowtiie.flashback.ui.decks.DeckEditorViewModel
import com.mowtiie.flashback.ui.decks.DeckListRoute
import com.mowtiie.flashback.ui.decks.DeckListViewModel

@Composable
fun FlashbackNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.DECKS) {
            deckDestinations(container, navController)
            placeholderDestinations(navController)
        }
        MessageHost(container, Modifier.align(Alignment.BottomCenter))
    }
}

private fun androidx.navigation.NavGraphBuilder.deckDestinations(
    container: AppContainer,
    navController: NavHostController,
) {
    composable(Routes.DECKS) {
        val viewModel: DeckListViewModel = viewModel(
            factory = viewModelFactory {
                initializer { DeckListViewModel(container.repository) }
            },
        )
        DeckListRoute(
            viewModel = viewModel,
            onOpenDeck = { navController.navigate(Routes.deckDetail(it)) },
            onStudyDeck = { navController.navigate(Routes.study(it)) },
            onAddDeck = { navController.navigate(Routes.deckEditor()) },
        )
    }

    composable(
        route = Routes.DECK_DETAIL,
        arguments = listOf(navArgument(Routes.DECK_ID) { type = NavType.LongType }),
    ) { entry ->
        val deckId = entry.longArgument(Routes.DECK_ID)
        val viewModel: DeckDetailViewModel = viewModel(
            factory = viewModelFactory {
                initializer { DeckDetailViewModel(container.repository, deckId) }
            },
        )
        DeckDetailRoute(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onEditDeck = { navController.navigate(Routes.deckEditor(deckId)) },
            onAddNote = { navController.navigate(Routes.noteEditor(deckId)) },
            onOpenNote = { navController.navigate(Routes.noteEditor(deckId, it)) },
            onStudy = { navController.navigate(Routes.study(deckId)) },
            onDeleted = { _, undo ->
                navController.popBackStack()
                container.messenger.showWithAction(R.string.deck_deleted, R.string.study_undo) {
                    undo.undo()
                }
            },
        )
    }

    composable(
        route = Routes.DECK_EDITOR,
        arguments = listOf(
            navArgument(Routes.DECK_ID) {
                type = NavType.LongType
                defaultValue = Routes.NEW_ID
            },
        ),
    ) { entry ->
        val deckId = entry.longArgument(Routes.DECK_ID)
        val viewModel: DeckEditorViewModel = viewModel(
            factory = viewModelFactory {
                initializer { DeckEditorViewModel(container.repository, deckId) }
            },
        )
        DeckEditorRoute(
            viewModel = viewModel,
            onClose = { navController.popBackStack() },
            onSaved = {
                navController.popBackStack()
                container.messenger.show(R.string.deck_saved)
            },
            onNewTag = { navController.navigate(Routes.tagEditor()) },
        )
    }
}

// Placeholders until each screen is ported.
private fun androidx.navigation.NavGraphBuilder.placeholderDestinations(
    navController: NavHostController,
) {
    composable(
        route = Routes.NOTE_EDITOR,
        arguments = listOf(
            navArgument(Routes.DECK_ID) { type = NavType.LongType },
            navArgument(Routes.NOTE_ID) {
                type = NavType.LongType
                defaultValue = Routes.NEW_ID
            },
        ),
    ) {
        PlaceholderScreen(Routes.NOTE_EDITOR, onBack = navController::popBackStack)
    }
    composable(
        route = Routes.TAG_EDITOR,
        arguments = listOf(
            navArgument(Routes.TAG_ID) {
                type = NavType.LongType
                defaultValue = Routes.NEW_ID
            },
        ),
    ) {
        PlaceholderScreen(Routes.TAG_EDITOR, onBack = navController::popBackStack)
    }
    composable(
        route = Routes.STUDY,
        arguments = listOf(navArgument(Routes.DECK_ID) { type = NavType.LongType }),
    ) {
        PlaceholderScreen(Routes.STUDY, onBack = navController::popBackStack)
    }
}

private fun NavBackStackEntry.longArgument(key: String): Long =
    requireNotNull(arguments) { "Missing arguments for $key" }.getLong(key)

/** Lives above the nav host so a message survives the screen that raised it being popped. */
@Composable
private fun MessageHost(container: AppContainer, modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(container, context) {
        container.messenger.messages.collect { message ->
            val result = hostState.showSnackbar(
                message = context.getString(message.text),
                actionLabel = message.actionLabel?.let(context::getString),
                duration = if (message.onAction == null) {
                    SnackbarDuration.Short
                } else {
                    SnackbarDuration.Long
                },
            )
            if (result == SnackbarResult.ActionPerformed) {
                message.onAction?.invoke()
            }
        }
    }

    SnackbarHost(hostState, modifier.navigationBarsPadding().imePadding())
}

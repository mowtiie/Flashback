package com.mowtiie.flashback.ui.navigation

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mowtiie.flashback.AppContainer
import com.mowtiie.flashback.R
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.ui.AppMessenger
import com.mowtiie.flashback.ui.decks.DeckDetailRoute
import com.mowtiie.flashback.ui.decks.DeckDetailViewModel
import com.mowtiie.flashback.ui.decks.DeckEditorRoute
import com.mowtiie.flashback.ui.decks.DeckEditorViewModel
import com.mowtiie.flashback.ui.decks.DeckListRoute
import com.mowtiie.flashback.ui.decks.DeckListViewModel
import com.mowtiie.flashback.ui.notes.NoteEditorRoute
import com.mowtiie.flashback.ui.notes.NoteEditorViewModel
import com.mowtiie.flashback.ui.settings.DataTransferViewModel
import com.mowtiie.flashback.ui.settings.SettingsRoute
import com.mowtiie.flashback.ui.settings.SettingsViewModel
import com.mowtiie.flashback.ui.stats.StatisticsRoute
import com.mowtiie.flashback.ui.stats.StatisticsViewModel
import com.mowtiie.flashback.ui.study.StudyRoute
import com.mowtiie.flashback.ui.study.StudyViewModel
import com.mowtiie.flashback.ui.tags.TagEditorRoute
import com.mowtiie.flashback.ui.tags.TagEditorViewModel
import com.mowtiie.flashback.ui.tags.TagsRoute
import com.mowtiie.flashback.ui.tags.TagsViewModel

private val deckIdArgument = navArgument(Routes.DECK_ID) { type = NavType.LongType }

private fun optionalIdArgument(name: String) = navArgument(name) {
    type = NavType.LongType
    defaultValue = Routes.NEW_ID
}

/** A ViewModel scoped to the current destination, built by hand since there is no DI framework. */
@Composable
private inline fun <reified VM : ViewModel> destinationViewModel(crossinline create: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { create() } })

fun NavGraphBuilder.flashbackGraph(container: AppContainer, navController: NavHostController) {
    deckDestinations(container, navController)
    noteDestinations(container, navController)
    tagDestinations(container, navController)
    statisticsDestination(container)
    settingsDestination(container)
}

private fun NavGraphBuilder.statisticsDestination(container: AppContainer) {
    composable(Routes.STATS) {
        StatisticsRoute(destinationViewModel { StatisticsViewModel(container.repository) })
    }
}

private fun NavGraphBuilder.settingsDestination(container: AppContainer) {
    composable(Routes.SETTINGS) {
        val settings = destinationViewModel {
            SettingsViewModel(
                studyPrefs = container.studyPreferences,
                themePrefs = container.themePreferences,
                reminderPrefs = container.reminderPreferences,
                scheduler = container.reminderScheduler,
                ioDispatcher = container.dispatchers.io,
                dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            )
        }
        val transfer = destinationViewModel {
            DataTransferViewModel(archive = container.repository, ioDispatcher = container.dispatchers.io)
        }
        SettingsRoute(settings, transfer)
    }
}

private fun NavGraphBuilder.deckDestinations(
    container: AppContainer,
    navController: NavHostController,
) {
    composable(Routes.DECKS) {
        DeckListRoute(
            viewModel = destinationViewModel { DeckListViewModel(container.repository) },
            onOpenDeck = { navController.navigate(Routes.deckDetail(it)) },
            onStudyDeck = { navController.navigate(Routes.study(it)) },
            onAddDeck = { navController.navigate(Routes.deckEditor()) },
        )
    }

    composable(Routes.STUDY, arguments = listOf(deckIdArgument)) { entry ->
        val deckId = entry.longArgument(Routes.DECK_ID)
        StudyRoute(
            viewModel = destinationViewModel {
                StudyViewModel(container.repository, deckId, container.studyPreferences.answerMode)
            },
            onBack = { navController.popBackStack() },
            onEditCard = { navController.navigate(Routes.noteEditor(deckId, it)) },
        )
    }

    composable(Routes.DECK_DETAIL, arguments = listOf(deckIdArgument)) { entry ->
        val deckId = entry.longArgument(Routes.DECK_ID)
        DeckDetailRoute(
            viewModel = destinationViewModel {
                DeckDetailViewModel(container.repository, deckId, container.dispatchers.io)
            },
            onBack = { navController.popBackStack() },
            onEditDeck = { navController.navigate(Routes.deckEditor(deckId)) },
            onAddNote = { navController.navigate(Routes.noteEditor(deckId)) },
            onOpenNote = { navController.navigate(Routes.noteEditor(deckId, it)) },
            onStudy = { navController.navigate(Routes.study(deckId)) },
            onDeleted = { _, undo ->
                navController.popBackStack()
                container.messenger.showUndoable(R.string.deck_deleted, undo)
            },
            onMessage = container.messenger::show,
        )
    }

    composable(
        route = Routes.DECK_EDITOR,
        arguments = listOf(optionalIdArgument(Routes.DECK_ID)),
    ) { entry ->
        val deckId = entry.longArgument(Routes.DECK_ID)
        DeckEditorRoute(
            viewModel = destinationViewModel { DeckEditorViewModel(container.repository, deckId) },
            onClose = { navController.popBackStack() },
            onSaved = {
                navController.popBackStack()
                container.messenger.show(R.string.deck_saved)
            },
            onNewTag = { navController.navigate(Routes.tagEditor()) },
        )
    }
}

private fun NavGraphBuilder.noteDestinations(
    container: AppContainer,
    navController: NavHostController,
) {
    composable(
        route = Routes.NOTE_EDITOR,
        arguments = listOf(deckIdArgument, optionalIdArgument(Routes.NOTE_ID)),
    ) { entry ->
        val deckId = entry.longArgument(Routes.DECK_ID)
        val noteId = entry.longArgument(Routes.NOTE_ID)
        NoteEditorRoute(
            viewModel = destinationViewModel { NoteEditorViewModel(container.repository, deckId, noteId) },
            onClose = { navController.popBackStack() },
            onSavedAnother = { container.messenger.show(R.string.note_saved) },
            onDeleted = { undo ->
                navController.popBackStack()
                container.messenger.showUndoable(R.string.note_deleted, undo)
            },
        )
    }
}

private fun NavGraphBuilder.tagDestinations(
    container: AppContainer,
    navController: NavHostController,
) {
    composable(Routes.TAGS) {
        TagsRoute(
            viewModel = destinationViewModel { TagsViewModel(container.repository) },
            onOpenTag = { navController.navigate(Routes.tagEditor(it)) },
            onAddTag = { navController.navigate(Routes.tagEditor()) },
        )
    }

    composable(
        route = Routes.TAG_EDITOR,
        arguments = listOf(optionalIdArgument(Routes.TAG_ID)),
    ) { entry ->
        val tagId = entry.longArgument(Routes.TAG_ID)
        TagEditorRoute(
            viewModel = destinationViewModel { TagEditorViewModel(container.repository, tagId) },
            onClose = { navController.popBackStack() },
            onDeleted = { undo ->
                navController.popBackStack()
                container.messenger.showUndoable(R.string.tag_deleted, undo)
            },
        )
    }
}

/** The "deleted" snackbar with an Undo that outlives the screen that raised it. */
private fun AppMessenger.showUndoable(@StringRes message: Int, undo: FlashbackRepository.UndoAction) =
    showWithAction(message, R.string.study_undo) { undo.undo() }

private fun NavBackStackEntry.longArgument(key: String): Long =
    requireNotNull(arguments) { "Missing arguments for $key" }.getLong(key)

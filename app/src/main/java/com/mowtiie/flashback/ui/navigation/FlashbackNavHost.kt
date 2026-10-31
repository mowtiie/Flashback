package com.mowtiie.flashback.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mowtiie.flashback.AppContainer
import com.mowtiie.flashback.ui.decks.DeckListRoute
import com.mowtiie.flashback.ui.decks.DeckListViewModel

@Composable
fun FlashbackNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.DECKS) {
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
                onAddDeck = { navController.navigate(Routes.DECK_NEW) },
            )
        }

        // Placeholders until each screen is ported.
        composable(Routes.DECK_NEW) {
            PlaceholderScreen(Routes.DECK_NEW, onBack = navController::popBackStack)
        }
        composable(
            route = Routes.DECK_DETAIL,
            arguments = listOf(navArgument(Routes.DECK_ID) { type = NavType.LongType }),
        ) {
            PlaceholderScreen(Routes.DECK_DETAIL, onBack = navController::popBackStack)
        }
        composable(
            route = Routes.STUDY,
            arguments = listOf(navArgument(Routes.DECK_ID) { type = NavType.LongType }),
        ) {
            PlaceholderScreen(Routes.STUDY, onBack = navController::popBackStack)
        }
    }
}

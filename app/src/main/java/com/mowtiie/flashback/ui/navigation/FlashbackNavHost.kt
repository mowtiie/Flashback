package com.mowtiie.flashback.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mowtiie.flashback.AppContainer
import com.mowtiie.flashback.ui.AppMessenger

/** Width at which the bottom bar becomes a side rail, whatever the orientation. */
private val RAIL_MIN_WIDTH = 600.dp

@Composable
fun FlashbackNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val topLevel = backStackEntry?.destination?.topLevel()
    val snackbarHostState = remember { SnackbarHostState() }

    MessageEffect(container.messenger, snackbarHostState)

    BoxWithConstraints {
        val wide = maxWidth >= RAIL_MIN_WIDTH
        val showRail = topLevel != null && wide
        val showBar = topLevel != null && !wide

        Row {
            if (showRail) {
                NavigationRail {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = destination == topLevel,
                            onClick = { navController.navigateTopLevel(destination) },
                            icon = { DestinationIcon(destination, selected = destination == topLevel) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (showRail) {
                            Modifier.consumeWindowInsets(
                                WindowInsets.systemBars.only(WindowInsetsSides.Start),
                            )
                        } else {
                            Modifier
                        },
                    ),
                // Each screen's own Scaffold handles the status bar; this one only hosts the bar.
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    if (showBar) {
                        NavigationBar {
                            TopLevelDestination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = destination == topLevel,
                                    onClick = { navController.navigateTopLevel(destination) },
                                    icon = { DestinationIcon(destination, selected = destination == topLevel) },
                                    label = { Text(stringResource(destination.label)) },
                                )
                            }
                        }
                    }
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        // With a bottom bar the Scaffold already lifts it clear of the nav bar.
                        modifier = if (showBar) Modifier else Modifier.navigationBarsPadding().imePadding(),
                    )
                },
            ) { inner ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.DECKS,
                    modifier = Modifier
                        .padding(inner)
                        .consumeWindowInsets(inner),
                ) {
                    flashbackGraph(container, navController)
                }
            }
        }
    }
}

private fun NavDestination.topLevel(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { top -> hierarchy.any { it.route == top.route } }

private fun NavHostController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Messages outlive the screen that raised them, e.g. "Deck deleted" after the detail screen pops. */
@Composable
private fun MessageEffect(messenger: AppMessenger, hostState: SnackbarHostState) {
    val resources = LocalResources.current

    LaunchedEffect(messenger, resources) {
        messenger.messages.collect { message ->
            val result = hostState.showSnackbar(
                message = resources.getString(message.text),
                actionLabel = message.actionLabel?.let(resources::getString),
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
}

/** Outlined while idle, filled once selected. */
@Composable
private fun DestinationIcon(destination: TopLevelDestination, selected: Boolean) {
    Icon(painterResource(if (selected) destination.selectedIcon else destination.icon), contentDescription = null)
}

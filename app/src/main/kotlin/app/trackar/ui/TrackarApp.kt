package app.trackar.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.trackar.core.model.SessionState
import app.trackar.feature.auth.LoginRoute
import app.trackar.feature.history.navigation.HistoryNavKey
import app.trackar.feature.history.navigation.historyEntry
import app.trackar.feature.map.navigation.MapNavKey
import app.trackar.feature.map.navigation.mapEntry
import app.trackar.feature.vehicles.navigation.VehicleDetailNavKey
import app.trackar.feature.vehicles.navigation.vehiclesEntries
import app.trackar.navigation.TopLevelDestination

/** Root: the session decides between the login flow and the authenticated app. */
@Composable
fun TrackarApp(sessionState: SessionState) {
    when (sessionState) {
        SessionState.Loading -> Unit

        // Splash screen is still visible.
        is SessionState.Loaded -> if (sessionState.session == null) LoginRoute() else AuthenticatedApp()
    }
}

@Composable
private fun AuthenticatedApp() {
    val backStack = rememberNavBackStack(MapNavKey)
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { it.key == backStack.firstOrNull() }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination == currentTopLevel
                item(
                    selected = selected,
                    onClick = { backStack.navigateToTopLevel(destination.key) },
                    icon = {
                        Icon(
                            if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(destination.label)) }
                )
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                val openDetails: (String) -> Unit = { id -> backStack.add(VehicleDetailNavKey(id)) }
                val openHistory: (String) -> Unit = { id -> backStack.add(HistoryNavKey(id)) }
                val back: () -> Unit = { backStack.removeLastOrNull() }
                mapEntry(onOpenDetails = openDetails, onOpenHistory = openHistory)
                vehiclesEntries(onVehicleClick = openDetails, onOpenHistory = openHistory, onBack = back)
                historyEntry(onBack = back)
            }
        )
    }
}

/** Switching tabs resets the stack to that tab's root (single-stack model). */
private fun NavBackStack<NavKey>.navigateToTopLevel(key: NavKey) {
    if (firstOrNull() == key && size == 1) return
    clear()
    add(key)
}

package app.trackar.feature.history.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.trackar.feature.history.HistoryRoute
import kotlinx.serialization.Serializable

@Serializable
data class HistoryNavKey(val vehicleId: String) : NavKey

fun EntryProviderScope<NavKey>.historyEntry(onBack: () -> Unit) {
    entry<HistoryNavKey> { key -> HistoryRoute(vehicleId = key.vehicleId, onBack = onBack) }
}

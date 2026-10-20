package app.trackar.feature.map.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.trackar.feature.map.MapRoute
import kotlinx.serialization.Serializable

@Serializable
data object MapNavKey : NavKey

fun EntryProviderScope<NavKey>.mapEntry(onOpenDetails: (String) -> Unit) {
    entry<MapNavKey> { MapRoute(onOpenDetails = onOpenDetails) }
}

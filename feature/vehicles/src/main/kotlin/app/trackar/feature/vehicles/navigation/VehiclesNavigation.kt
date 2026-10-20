package app.trackar.feature.vehicles.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import app.trackar.feature.vehicles.detail.VehicleDetailRoute
import app.trackar.feature.vehicles.list.VehicleListRoute
import kotlinx.serialization.Serializable

@Serializable
data object VehicleListNavKey : NavKey

@Serializable
data class VehicleDetailNavKey(val vehicleId: String) : NavKey

fun EntryProviderScope<NavKey>.vehiclesEntries(onVehicleClick: (String) -> Unit, onBack: () -> Unit) {
    entry<VehicleListNavKey> { VehicleListRoute(onVehicleClick = onVehicleClick) }
    entry<VehicleDetailNavKey> { key -> VehicleDetailRoute(vehicleId = key.vehicleId, onBack = onBack) }
}

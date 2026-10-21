package app.trackar.feature.map

import app.trackar.core.model.ConnectionState
import app.trackar.core.model.DeviceStatus
import app.trackar.core.model.Vehicle

sealed interface MapUiState {
    data object Loading : MapUiState

    data class Ready(
        /** Only vehicles with a known position can be plotted. */
        val vehicles: List<Vehicle>,
        val selected: Vehicle?,
        val connection: ConnectionState
    ) : MapUiState {
        val onlineCount: Int get() = vehicles.count { it.status == DeviceStatus.ONLINE }
    }
}

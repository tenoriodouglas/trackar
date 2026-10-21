package app.trackar.feature.history

import app.trackar.core.model.TripSummary
import app.trackar.core.model.VehiclePosition
import java.time.LocalDate

data class HistoryUiState(
    val vehicleName: String = "",
    val date: LocalDate,
    val today: LocalDate,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val positions: List<VehiclePosition> = emptyList(),
    val summary: TripSummary? = null,
    val playbackIndex: Int = 0,
    val isPlaying: Boolean = false
) {
    val canGoForward: Boolean get() = date < today
    val isEmpty: Boolean get() = !isLoading && !hasError && positions.isEmpty()

    /** Position shown by the playback cursor. */
    val current: VehiclePosition? get() = positions.getOrNull(playbackIndex)
}

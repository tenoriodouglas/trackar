package app.trackar.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MapViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val uiState: StateFlow<MapUiState> = combine(
        vehicleRepository.observeVehicles(),
        selectedId,
        vehicleRepository.connectionState
    ) { vehicles, selectedId, connection ->
        val plotted = vehicles.filter { it.position != null }
        MapUiState.Ready(
            vehicles = plotted,
            selected = plotted.firstOrNull { it.id == selectedId },
            connection = connection
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState.Loading)

    fun onVehicleSelected(id: String?) {
        savedStateHandle[KEY_SELECTED] = id
    }

    private companion object {
        const val KEY_SELECTED = "selected"
    }
}

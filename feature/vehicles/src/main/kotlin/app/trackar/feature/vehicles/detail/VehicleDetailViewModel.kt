package app.trackar.feature.vehicles.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.VehicleRepository
import app.trackar.core.model.Vehicle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface VehicleDetailUiState {
    data object Loading : VehicleDetailUiState
    data object NotFound : VehicleDetailUiState
    data class Ready(val vehicle: Vehicle) : VehicleDetailUiState
}

@HiltViewModel(assistedFactory = VehicleDetailViewModel.Factory::class)
class VehicleDetailViewModel @AssistedInject constructor(
    vehicleRepository: VehicleRepository,
    @Assisted vehicleId: String
) : ViewModel() {

    val uiState: StateFlow<VehicleDetailUiState> = vehicleRepository.observeVehicle(vehicleId)
        .map { vehicle -> vehicle?.let(VehicleDetailUiState::Ready) ?: VehicleDetailUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleDetailUiState.Loading)

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: String): VehicleDetailViewModel
    }
}

package app.trackar.feature.vehicles.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.VehicleRepository
import app.trackar.core.model.CommandRejectedException
import app.trackar.core.model.CommandType
import app.trackar.core.model.NetworkUnavailableException
import app.trackar.core.model.Vehicle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface VehicleDetailUiState {
    data object Loading : VehicleDetailUiState
    data object NotFound : VehicleDetailUiState
    data class Ready(val vehicle: Vehicle) : VehicleDetailUiState
}

/** Transient state of user actions (commands, unlinking). */
data class DetailActionState(
    val sending: Boolean = false,
    val error: DetailError? = null,
    /** Set once the vehicle was unlinked; the screen navigates back. */
    val released: Boolean = false
)

enum class DetailError { TOO_SOON, NOT_ALLOWED, NETWORK, UNKNOWN }

@HiltViewModel(assistedFactory = VehicleDetailViewModel.Factory::class)
class VehicleDetailViewModel @AssistedInject constructor(
    private val vehicleRepository: VehicleRepository,
    @Assisted private val vehicleId: String
) : ViewModel() {

    val uiState: StateFlow<VehicleDetailUiState> = vehicleRepository.observeVehicle(vehicleId)
        .map { vehicle -> vehicle?.let(VehicleDetailUiState::Ready) ?: VehicleDetailUiState.NotFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleDetailUiState.Loading)

    private val _actionState = MutableStateFlow(DetailActionState())
    val actionState: StateFlow<DetailActionState> = _actionState.asStateFlow()

    fun sendCommand(type: CommandType) = runAction { vehicleRepository.sendCommand(vehicleId, type) }

    fun release() = runAction {
        vehicleRepository.releaseVehicle(vehicleId).onSuccess { _actionState.update { it.copy(released = true) } }
    }

    fun dismissError() = _actionState.update { it.copy(error = null) }

    private fun runAction(block: suspend () -> Result<Unit>) {
        if (_actionState.value.sending) return
        _actionState.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val error = block().exceptionOrNull()?.toDetailError()
            _actionState.update { it.copy(sending = false, error = error) }
        }
    }

    private fun Throwable.toDetailError(): DetailError = when (this) {
        is CommandRejectedException -> when (reason) {
            CommandRejectedException.Reason.TOO_SOON -> DetailError.TOO_SOON
            CommandRejectedException.Reason.NOT_ALLOWED -> DetailError.NOT_ALLOWED
            CommandRejectedException.Reason.UNKNOWN -> DetailError.UNKNOWN
        }

        is NetworkUnavailableException -> DetailError.NETWORK

        else -> DetailError.UNKNOWN
    }

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: String): VehicleDetailViewModel
    }
}

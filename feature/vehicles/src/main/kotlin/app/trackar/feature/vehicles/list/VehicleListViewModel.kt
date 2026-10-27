package app.trackar.feature.vehicles.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.AuthRepository
import app.trackar.core.data.repository.VehicleRepository
import app.trackar.core.model.InvalidTrackerIdException
import app.trackar.core.model.TrackerAlreadyClaimedException
import app.trackar.core.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface VehicleListUiState {
    data object Loading : VehicleListUiState
    data class Ready(val vehicles: List<Vehicle>, val query: String) : VehicleListUiState
}

sealed interface AddTrackerState {
    data object Hidden : AddTrackerState
    data object Editing : AddTrackerState
    data object Saving : AddTrackerState
    data class Error(val reason: AddTrackerError) : AddTrackerState
}

enum class AddTrackerError { ALREADY_CLAIMED, INVALID_ID, UNKNOWN }

@HiltViewModel
class VehicleListViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val uiState: StateFlow<VehicleListUiState> = combine(vehicleRepository.observeVehicles(), query) { vehicles, q ->
        VehicleListUiState.Ready(vehicles = vehicles.filterBy(q), query = q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleListUiState.Loading)

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            vehicleRepository.refresh()
            _isRefreshing.value = false
        }
    }

    private val _addTracker = MutableStateFlow<AddTrackerState>(AddTrackerState.Hidden)
    val addTracker: StateFlow<AddTrackerState> = _addTracker.asStateFlow()

    fun showAddTracker() = _addTracker.update { AddTrackerState.Editing }

    fun hideAddTracker() = _addTracker.update { AddTrackerState.Hidden }

    fun addTracker(imei: String, name: String) {
        if (_addTracker.value is AddTrackerState.Saving) return
        _addTracker.value = AddTrackerState.Saving
        viewModelScope.launch {
            _addTracker.value = vehicleRepository.claimVehicle(imei, name).fold(
                onSuccess = { AddTrackerState.Hidden },
                onFailure = {
                    AddTrackerState.Error(
                        when (it) {
                            is TrackerAlreadyClaimedException -> AddTrackerError.ALREADY_CLAIMED
                            is InvalidTrackerIdException -> AddTrackerError.INVALID_ID
                            else -> AddTrackerError.UNKNOWN
                        }
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private fun List<Vehicle>.filterBy(query: String): List<Vehicle> {
        val q = query.trim()
        if (q.isEmpty()) return this
        return filter { it.name.contains(q, ignoreCase = true) || it.uniqueId.contains(q, ignoreCase = true) }
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}

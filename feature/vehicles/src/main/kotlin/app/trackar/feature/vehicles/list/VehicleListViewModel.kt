package app.trackar.feature.vehicles.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.AuthRepository
import app.trackar.core.data.repository.VehicleRepository
import app.trackar.core.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VehicleListUiState {
    data object Loading : VehicleListUiState
    data class Ready(val vehicles: List<Vehicle>, val query: String) : VehicleListUiState
}

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

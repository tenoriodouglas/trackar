package app.trackar.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trackar.core.data.repository.VehicleRepository
import app.trackar.core.model.summarize
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = HistoryViewModel.Factory::class)
class HistoryViewModel @AssistedInject constructor(
    private val vehicleRepository: VehicleRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
    @Assisted private val vehicleId: String
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now(clock)

    private val _uiState = MutableStateFlow(
        HistoryUiState(
            date = savedStateHandle.get<Long>(KEY_DATE)?.let(LocalDate::ofEpochDay) ?: today,
            today = today
        )
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            vehicleRepository.observeVehicle(vehicleId).collect { vehicle ->
                _uiState.update { it.copy(vehicleName = vehicle?.name.orEmpty()) }
            }
        }
        load()
    }

    fun selectDate(date: LocalDate) {
        val clamped = minOf(date, today)
        if (clamped == _uiState.value.date) return
        savedStateHandle[KEY_DATE] = clamped.toEpochDay()
        _uiState.update { it.copy(date = clamped, today = today) }
        load()
    }

    fun previousDay() = selectDate(_uiState.value.date.minusDays(1))

    fun nextDay() = selectDate(_uiState.value.date.plusDays(1))

    fun retry() = load()

    fun seekTo(index: Int) {
        pause()
        _uiState.update { it.copy(playbackIndex = index.coerceIn(0, (it.positions.size - 1).coerceAtLeast(0))) }
    }

    fun togglePlayback() = if (_uiState.value.isPlaying) pause() else play()

    private fun play() {
        val size = _uiState.value.positions.size
        if (size < 2) return
        // Restart from the beginning when the cursor is already at the end.
        if (_uiState.value.playbackIndex >= size - 1) _uiState.update { it.copy(playbackIndex = 0) }
        val step = (size / PLAYBACK_FRAMES).coerceAtLeast(1)
        _uiState.update { it.copy(isPlaying = true) }
        playbackJob = viewModelScope.launch {
            while (isActive) {
                delay(PLAYBACK_TICK_MS)
                val next = (_uiState.value.playbackIndex + step).coerceAtMost(size - 1)
                _uiState.update { it.copy(playbackIndex = next) }
                if (next == size - 1) break
            }
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    private fun pause() {
        playbackJob?.cancel()
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun load() {
        pause()
        loadJob?.cancel()
        val date = _uiState.value.date
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            val zone = clock.zone
            val from = date.atStartOfDay(zone).toInstant()
            val to = date.plusDays(1).atStartOfDay(zone).toInstant()
            vehicleRepository.getHistory(vehicleId, from, to).fold(
                onSuccess = { positions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            positions = positions,
                            summary = positions.summarize(),
                            playbackIndex = (positions.size - 1).coerceAtLeast(0)
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, hasError = true, positions = emptyList(), summary = null)
                    }
                }
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: String): HistoryViewModel
    }

    private companion object {
        const val KEY_DATE = "date"
        const val PLAYBACK_TICK_MS = 80L

        /** Whole-day playback lasts roughly PLAYBACK_FRAMES * PLAYBACK_TICK_MS (~20 s). */
        const val PLAYBACK_FRAMES = 250
    }
}

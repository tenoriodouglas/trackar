package app.trackar.feature.history

import androidx.lifecycle.SavedStateHandle
import app.trackar.core.testing.MainDispatcherRule
import app.trackar.core.testing.TestVehicleRepository
import app.trackar.core.testing.testVehicle
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val clock = Clock.fixed(Instant.parse("2026-03-10T15:00:00Z"), ZoneOffset.UTC)
    private val repository = TestVehicleRepository()

    private fun viewModel() = HistoryViewModel(repository, clock, SavedStateHandle(), "1")

    @Test
    fun `loads today by default with day boundaries`() = runTest(dispatcher) {
        val positions = List(3) { testVehicle("1").position!!.copy(fixTime = Instant.parse("2026-03-10T10:0$it:00Z")) }
        repository.historyResult = Result.success(positions)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(LocalDate.of(2026, 3, 10), state.date)
        assertEquals(
            Instant.parse("2026-03-10T00:00:00Z") to Instant.parse("2026-03-11T00:00:00Z"),
            repository.lastHistoryRange
        )
        assertEquals(3, state.positions.size)
        assertEquals(2, state.playbackIndex)
        assertFalse(state.canGoForward)
    }

    @Test
    fun `cannot navigate past today`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.nextDay()
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 3, 10), vm.uiState.value.date)

        vm.previousDay()
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 3, 9), vm.uiState.value.date)
        assertTrue(vm.uiState.value.canGoForward)
    }

    @Test
    fun `empty and error states`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)

        repository.historyResult = Result.failure(IOException())
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasError)
    }

    @Test
    fun `playback runs to the end and stops`() = runTest(dispatcher) {
        val positions = List(5) { testVehicle("1").position!! }
        repository.historyResult = Result.success(positions)
        val vm = viewModel()
        advanceUntilIdle()

        vm.seekTo(0)
        vm.togglePlayback()
        assertTrue(vm.uiState.value.isPlaying)
        advanceUntilIdle()

        assertEquals(4, vm.uiState.value.playbackIndex)
        assertFalse(vm.uiState.value.isPlaying)
    }
}

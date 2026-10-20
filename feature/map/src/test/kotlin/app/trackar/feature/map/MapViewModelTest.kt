package app.trackar.feature.map

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.trackar.core.testing.MainDispatcherRule
import app.trackar.core.testing.TestVehicleRepository
import app.trackar.core.testing.testVehicle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = TestVehicleRepository()
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        viewModel = MapViewModel(repository, SavedStateHandle())
    }

    @Test
    fun `initial state is loading`() {
        assertEquals(MapUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `vehicles without position are not plotted`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            repository.sendVehicles(listOf(testVehicle("1"), testVehicle("2", withPosition = false)))
            val state = awaitItem() as MapUiState.Ready
            assertEquals(listOf("1"), state.vehicles.map { it.id })
        }
    }

    @Test
    fun `selection follows live updates and can be cleared`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            repository.sendVehicles(listOf(testVehicle("1")))
            skipItems(1)

            viewModel.onVehicleSelected("1")
            assertEquals("1", (awaitItem() as MapUiState.Ready).selected?.id)

            viewModel.onVehicleSelected(null)
            assertNull((awaitItem() as MapUiState.Ready).selected)
        }
    }
}

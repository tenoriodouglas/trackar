package app.trackar.feature.vehicles

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.trackar.core.model.SessionState
import app.trackar.core.model.TrackerAlreadyClaimedException
import app.trackar.core.testing.MainDispatcherRule
import app.trackar.core.testing.TestAuthRepository
import app.trackar.core.testing.TestVehicleRepository
import app.trackar.core.testing.testVehicle
import app.trackar.feature.vehicles.list.AddTrackerError
import app.trackar.feature.vehicles.list.AddTrackerState
import app.trackar.feature.vehicles.list.VehicleListUiState
import app.trackar.feature.vehicles.list.VehicleListViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VehicleListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vehicleRepository = TestVehicleRepository()
    private val authRepository = TestAuthRepository()
    private lateinit var viewModel: VehicleListViewModel

    @Before
    fun setUp() {
        viewModel = VehicleListViewModel(vehicleRepository, authRepository, SavedStateHandle())
    }

    @Test
    fun `query filters by name or unique id`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            vehicleRepository.sendVehicles(listOf(testVehicle("1", name = "Hilux"), testVehicle("2", name = "Strada")))
            skipItems(1)

            viewModel.onQueryChange("hil")
            assertEquals(listOf("1"), (awaitItem() as VehicleListUiState.Ready).vehicles.map { it.id })

            viewModel.onQueryChange("IMEI-2")
            assertEquals(listOf("2"), (awaitItem() as VehicleListUiState.Ready).vehicles.map { it.id })
        }
    }

    @Test
    fun `refresh delegates to repository`() = runTest {
        viewModel.refresh()
        assertEquals(1, vehicleRepository.refreshCount)
        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `adding a tracker claims it and closes the dialog`() = runTest {
        viewModel.showAddTracker()
        viewModel.addTracker("359339075123456", "Hilux")

        assertEquals(listOf("359339075123456" to "Hilux"), vehicleRepository.claims)
        assertEquals(AddTrackerState.Hidden, viewModel.addTracker.value)
    }

    @Test
    fun `claimed tracker shows a specific error`() = runTest {
        vehicleRepository.claimResult = Result.failure(TrackerAlreadyClaimedException())
        viewModel.addTracker("359339075123456", "")

        assertEquals(AddTrackerState.Error(AddTrackerError.ALREADY_CLAIMED), viewModel.addTracker.value)
    }

    @Test
    fun `logout clears session`() = runTest {
        authRepository.login("e", "p")
        viewModel.logout()
        assertEquals(SessionState.Loaded(null), authRepository.sessionState.first())
    }
}

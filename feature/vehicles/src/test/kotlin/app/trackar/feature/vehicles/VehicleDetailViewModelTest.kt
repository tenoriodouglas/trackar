package app.trackar.feature.vehicles

import app.trackar.core.model.CommandRejectedException
import app.trackar.core.model.CommandType
import app.trackar.core.testing.MainDispatcherRule
import app.trackar.core.testing.TestVehicleRepository
import app.trackar.feature.vehicles.detail.DetailError
import app.trackar.feature.vehicles.detail.VehicleDetailViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VehicleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = TestVehicleRepository()
    private lateinit var viewModel: VehicleDetailViewModel

    @Before
    fun setUp() {
        viewModel = VehicleDetailViewModel(repository, "1")
    }

    @Test
    fun `sends block command for this vehicle`() = runTest {
        viewModel.sendCommand(CommandType.ENGINE_STOP)

        assertEquals(listOf("1" to CommandType.ENGINE_STOP), repository.sentCommands)
        assertFalse(viewModel.actionState.value.sending)
        assertNull(viewModel.actionState.value.error)
    }

    @Test
    fun `backend rejection is translated`() = runTest {
        repository.commandResult =
            Result.failure(CommandRejectedException(CommandRejectedException.Reason.VEHICLE_MOVING))
        viewModel.sendCommand(CommandType.ENGINE_STOP)
        assertEquals(DetailError.VEHICLE_MOVING, viewModel.actionState.value.error)

        viewModel.dismissError()
        assertNull(viewModel.actionState.value.error)
    }

    @Test
    fun `release unlinks and signals navigation`() = runTest {
        viewModel.release()
        assertEquals(listOf("1"), repository.released)
        assertTrue(viewModel.actionState.value.released)
    }
}

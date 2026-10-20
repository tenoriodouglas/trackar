package app.trackar.core.testing

import app.trackar.core.data.repository.VehicleRepository
import app.trackar.core.model.ConnectionState
import app.trackar.core.model.Vehicle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TestVehicleRepository : VehicleRepository {
    private val vehicles = MutableSharedFlow<List<Vehicle>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val connectionState = MutableStateFlow(ConnectionState.CONNECTED)

    var refreshCount = 0
        private set

    fun sendVehicles(list: List<Vehicle>) {
        vehicles.tryEmit(list)
    }

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles

    override fun observeVehicle(id: String): Flow<Vehicle?> = vehicles.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refresh(): Result<Unit> {
        refreshCount++
        return Result.success(Unit)
    }
}

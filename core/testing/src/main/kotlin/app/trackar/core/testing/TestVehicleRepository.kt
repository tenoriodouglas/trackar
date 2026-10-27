package app.trackar.core.testing

import app.trackar.core.data.repository.VehicleRepository
import app.trackar.core.model.CommandType
import app.trackar.core.model.ConnectionState
import app.trackar.core.model.Vehicle
import app.trackar.core.model.VehiclePosition
import java.time.Instant
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

    /** Result returned by [getHistory]; the last requested range is recorded. */
    var historyResult: Result<List<VehiclePosition>> = Result.success(emptyList())
    var lastHistoryRange: Pair<Instant, Instant>? = null
        private set

    override suspend fun getHistory(vehicleId: String, from: Instant, to: Instant): Result<List<VehiclePosition>> {
        lastHistoryRange = from to to
        return historyResult
    }

    /** Results returned by the action methods; requests are recorded for assertions. */
    var commandResult: Result<Unit> = Result.success(Unit)
    var claimResult: Result<Unit> = Result.success(Unit)
    val sentCommands = mutableListOf<Pair<String, CommandType>>()
    val claims = mutableListOf<Pair<String, String?>>()
    val released = mutableListOf<String>()

    override suspend fun sendCommand(vehicleId: String, type: CommandType): Result<Unit> {
        sentCommands += vehicleId to type
        return commandResult
    }

    override suspend fun claimVehicle(imei: String, name: String?): Result<Unit> {
        claims += imei to name
        return claimResult
    }

    override suspend fun releaseVehicle(vehicleId: String): Result<Unit> {
        released += vehicleId
        return Result.success(Unit)
    }

    override suspend fun refresh(): Result<Unit> {
        refreshCount++
        return Result.success(Unit)
    }
}

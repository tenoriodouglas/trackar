package app.trackar.core.data.repository

import app.trackar.core.model.ConnectionState
import app.trackar.core.model.Vehicle
import app.trackar.core.model.VehiclePosition
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {
    /** Whether the data on screen is confirmed by the server or only cached. */
    val connectionState: StateFlow<ConnectionState>

    /** Real-time list of the signed-in user's vehicles (cache first, then live). */
    fun observeVehicles(): Flow<List<Vehicle>>

    fun observeVehicle(id: String): Flow<Vehicle?>

    /** Forces a server round-trip; live updates arrive through [observeVehicles]. */
    suspend fun refresh(): Result<Unit>

    /** Position history of one vehicle in [from, to), oldest first. */
    suspend fun getHistory(vehicleId: String, from: Instant, to: Instant): Result<List<VehiclePosition>>
}

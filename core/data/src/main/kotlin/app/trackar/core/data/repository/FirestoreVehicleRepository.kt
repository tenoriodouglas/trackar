package app.trackar.core.data.repository

import app.trackar.core.common.di.ApplicationScope
import app.trackar.core.common.result.suspendRunCatching
import app.trackar.core.firebase.AuthDataSource
import app.trackar.core.firebase.VehicleActionsDataSource
import app.trackar.core.firebase.VehicleDataSource
import app.trackar.core.model.CommandType
import app.trackar.core.model.ConnectionState
import app.trackar.core.model.Vehicle
import app.trackar.core.model.VehiclePosition
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

/**
 * Firestore is the single source of truth (its offline cache doubles as local storage).
 * One listener is shared by all screens and lives only while someone observes.
 */
@Singleton
internal class FirestoreVehicleRepository @Inject constructor(
    private val authDataSource: AuthDataSource,
    private val vehicleDataSource: VehicleDataSource,
    private val vehicleActions: VehicleActionsDataSource,
    @ApplicationScope appScope: CoroutineScope
) : VehicleRepository {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val vehicles: Flow<List<Vehicle>> = authDataSource.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                vehicleDataSource.observeVehicles(userId)
                    .onStart { _connectionState.value = ConnectionState.CONNECTING }
                    .onEach { snapshot ->
                        _connectionState.value =
                            if (snapshot.fromCache) ConnectionState.DISCONNECTED else ConnectionState.CONNECTED
                    }
                    .map { it.vehicles }
                    // e.g. permission denied after sign-out; keep the UI alive with an empty list.
                    .catch { emit(emptyList()) }
            }
        }
        .onCompletion { _connectionState.value = ConnectionState.DISCONNECTED }
        .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    override fun observeVehicles(): Flow<List<Vehicle>> = vehicles

    override fun observeVehicle(id: String): Flow<Vehicle?> =
        vehicles.map { list -> list.firstOrNull { it.id == id } }.distinctUntilChanged()

    override suspend fun refresh(): Result<Unit> = suspendRunCatching {
        val userId = checkNotNull(authDataSource.session.first()?.userId) { "Not signed in" }
        vehicleDataSource.fetchVehicles(userId)
    }

    override suspend fun getHistory(vehicleId: String, from: Instant, to: Instant): Result<List<VehiclePosition>> =
        suspendRunCatching { vehicleDataSource.fetchHistory(vehicleId, from, to) }

    override suspend fun sendCommand(vehicleId: String, type: CommandType): Result<Unit> =
        suspendRunCatching { vehicleActions.sendCommand(vehicleId, type) }

    override suspend fun claimVehicle(imei: String, name: String?): Result<Unit> =
        suspendRunCatching { vehicleActions.claim(imei.trim(), name?.trim()?.takeIf(String::isNotEmpty)) }

    override suspend fun releaseVehicle(vehicleId: String): Result<Unit> =
        suspendRunCatching { vehicleActions.release(vehicleId) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

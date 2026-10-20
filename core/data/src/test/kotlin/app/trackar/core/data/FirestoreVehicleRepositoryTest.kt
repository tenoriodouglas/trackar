package app.trackar.core.data

import app.cash.turbine.test
import app.trackar.core.data.repository.FirestoreVehicleRepository
import app.trackar.core.firebase.AuthDataSource
import app.trackar.core.firebase.VehicleDataSource
import app.trackar.core.firebase.VehicleSnapshot
import app.trackar.core.model.ConnectionState
import app.trackar.core.model.DeviceStatus
import app.trackar.core.model.Session
import app.trackar.core.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FirestoreVehicleRepositoryTest {

    private val auth = FakeAuthDataSource()
    private val dataSource = FakeVehicleDataSource()

    @Test
    fun `emits vehicles of the signed-in user and tracks connection`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        auth.session.value = Session("u1", "a@b.com")

        repository.observeVehicles().test {
            dataSource.emit(VehicleSnapshot(listOf(vehicle("1")), fromCache = true))
            assertEquals(listOf("1"), awaitItem().map { it.id })
            assertEquals(ConnectionState.DISCONNECTED, repository.connectionState.value)

            dataSource.emit(VehicleSnapshot(listOf(vehicle("1"), vehicle("2")), fromCache = false))
            assertEquals(listOf("1", "2"), awaitItem().map { it.id })
            assertEquals(ConnectionState.CONNECTED, repository.connectionState.value)
            assertEquals("u1", dataSource.lastUserId)
        }
    }

    @Test
    fun `signing out clears the list`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        auth.session.value = Session("u1", null)

        repository.observeVehicles().test {
            dataSource.emit(VehicleSnapshot(listOf(vehicle("1")), fromCache = false))
            assertEquals(1, awaitItem().size)

            auth.session.value = null
            assertEquals(emptyList<Vehicle>(), awaitItem())
        }
    }

    private fun TestScope.repository() = FirestoreVehicleRepository(auth, dataSource, backgroundScope)

    private fun vehicle(id: String) = Vehicle(id, "V$id", id, DeviceStatus.ONLINE, null, null, null)

    private class FakeAuthDataSource : AuthDataSource {
        override val session = MutableStateFlow<Session?>(null)
        override suspend fun signIn(email: String, password: String) = Unit
        override fun signOut() {
            session.value = null
        }
    }

    private class FakeVehicleDataSource : VehicleDataSource {
        private val snapshots = MutableSharedFlow<VehicleSnapshot>()
        var lastUserId: String? = null

        suspend fun emit(snapshot: VehicleSnapshot) = snapshots.emit(snapshot)

        override fun observeVehicles(userId: String): Flow<VehicleSnapshot> {
            lastUserId = userId
            return snapshots
        }

        override suspend fun fetchVehicles(userId: String): List<Vehicle> = emptyList()
    }
}

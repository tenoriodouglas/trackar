package app.trackar.core.firebase

import app.trackar.core.model.Vehicle
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** A batch of vehicles plus whether it is confirmed by the server or served from the local cache. */
data class VehicleSnapshot(val vehicles: List<Vehicle>, val fromCache: Boolean)

interface VehicleDataSource {
    /** Real-time stream of the user's vehicles; listener is removed on cancellation. */
    fun observeVehicles(userId: String): Flow<VehicleSnapshot>

    /** Forces a server round-trip (pull-to-refresh). */
    suspend fun fetchVehicles(userId: String): List<Vehicle>
}

internal class FirestoreVehicleDataSource @Inject constructor(private val firestore: FirebaseFirestore) :
    VehicleDataSource {

    private fun query(userId: String) =
        firestore.collection(VehicleFields.COLLECTION).whereArrayContains(VehicleFields.OWNER_IDS, userId)

    override fun observeVehicles(userId: String): Flow<VehicleSnapshot> = callbackFlow {
        // Metadata changes let us report cache -> server transitions as connection state.
        val registration = query(userId).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> trySend(VehicleSnapshot(snapshot.toVehicles(), snapshot.metadata.isFromCache))
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun fetchVehicles(userId: String): List<Vehicle> =
        query(userId).get(Source.SERVER).await().toVehicles()

    private fun QuerySnapshot.toVehicles(): List<Vehicle> = documents.mapNotNull { doc ->
        doc.data?.let { vehicleFromDocument(doc.id, it) }
    }.sortedBy { it.name.lowercase() }
}

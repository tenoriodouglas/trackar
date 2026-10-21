package app.trackar.core.firebase

import app.trackar.core.model.Vehicle
import app.trackar.core.model.VehiclePosition
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import java.time.Instant
import java.util.Date
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

    /** Chronological fixes in [from, to), capped at [MAX_HISTORY_POINTS]. */
    suspend fun fetchHistory(vehicleId: String, from: Instant, to: Instant): List<VehiclePosition>

    companion object {
        const val MAX_HISTORY_POINTS = 5_000L
    }
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

    override suspend fun fetchHistory(vehicleId: String, from: Instant, to: Instant): List<VehiclePosition> =
        firestore.collection(VehicleFields.COLLECTION).document(vehicleId).collection(VehicleFields.HISTORY)
            .whereGreaterThanOrEqualTo(VehicleFields.FIX_TIME, from.toTimestamp())
            .whereLessThan(VehicleFields.FIX_TIME, to.toTimestamp())
            .orderBy(VehicleFields.FIX_TIME, Query.Direction.ASCENDING)
            .limit(VehicleDataSource.MAX_HISTORY_POINTS)
            .get()
            .await()
            .documents
            .mapNotNull { it.data?.toPosition(vehicleId) }

    private fun Instant.toTimestamp() = Timestamp(Date.from(this))

    private fun QuerySnapshot.toVehicles(): List<Vehicle> = documents.mapNotNull { doc ->
        doc.data?.let { vehicleFromDocument(doc.id, it) }
    }.sortedBy { it.name.lowercase() }
}

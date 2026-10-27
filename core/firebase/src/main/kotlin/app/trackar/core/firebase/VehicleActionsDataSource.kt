package app.trackar.core.firebase

import app.trackar.core.model.CommandRejectedException
import app.trackar.core.model.CommandType
import app.trackar.core.model.InvalidTrackerIdException
import app.trackar.core.model.NetworkUnavailableException
import app.trackar.core.model.TrackerAlreadyClaimedException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctionsException.Code
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** Server-validated actions on vehicles (Cloud Functions callables). */
interface VehicleActionsDataSource {
    suspend fun sendCommand(vehicleId: String, type: CommandType)

    suspend fun claim(imei: String, name: String?)

    suspend fun release(vehicleId: String)
}

internal class CallableVehicleActionsDataSource @Inject constructor(private val functions: FirebaseFunctions) :
    VehicleActionsDataSource {

    override suspend fun sendCommand(vehicleId: String, type: CommandType) = call(
        "sendCommand",
        mapOf("vehicleId" to vehicleId, "type" to type.wireName)
    ) { code, message ->
        when (code) {
            Code.FAILED_PRECONDITION -> CommandRejectedException(
                CommandRejectedException.Reason.VEHICLE_MOVING,
                message
            )

            Code.RESOURCE_EXHAUSTED -> CommandRejectedException(CommandRejectedException.Reason.TOO_SOON, message)

            Code.PERMISSION_DENIED -> CommandRejectedException(CommandRejectedException.Reason.NOT_ALLOWED, message)

            else -> null
        }
    }

    override suspend fun claim(imei: String, name: String?) = call(
        "claimVehicle",
        buildMap {
            put("imei", imei)
            name?.let { put("name", it) }
        }
    ) { code, _ ->
        when (code) {
            Code.ALREADY_EXISTS -> TrackerAlreadyClaimedException()
            Code.INVALID_ARGUMENT -> InvalidTrackerIdException()
            else -> null
        }
    }

    override suspend fun release(vehicleId: String) = call("releaseVehicle", mapOf("vehicleId" to vehicleId)) { _, _ ->
        null
    }

    /** Calls a function and maps its error codes to domain exceptions via [mapError]. */
    private suspend fun call(name: String, data: Map<String, Any>, mapError: (Code, String?) -> Exception?) {
        try {
            functions.getHttpsCallable(name).call(data).await()
        } catch (e: FirebaseFunctionsException) {
            throw mapError(e.code, e.message)
                ?: if (e.code == Code.UNAVAILABLE ||
                    e.code == Code.DEADLINE_EXCEEDED
                ) {
                    NetworkUnavailableException(e)
                } else {
                    e
                }
        }
    }
}

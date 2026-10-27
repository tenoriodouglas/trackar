package app.trackar.core.firebase

import app.trackar.core.model.CommandStatus
import app.trackar.core.model.CommandType
import app.trackar.core.model.Coordinates
import app.trackar.core.model.DeviceStatus
import app.trackar.core.model.Vehicle
import app.trackar.core.model.VehicleCommand
import app.trackar.core.model.VehiclePosition
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.time.Instant

/**
 * Firestore schema for `vehicles/{id}` (written by the device gateway, read-only for the app).
 * Mapped by hand: no reflection, R8-safe, and tolerant to missing/malformed fields.
 */
internal object VehicleFields {
    const val COLLECTION = "vehicles"
    const val OWNER_IDS = "ownerIds"
    const val NAME = "name"
    const val UNIQUE_ID = "uniqueId"
    const val STATUS = "status"
    const val LAST_UPDATE = "lastUpdate"
    const val CATEGORY = "category"
    const val POSITION = "position"

    /** Subcollection `vehicles/{id}/positions/{autoId}`: one document per fix (same fields as [POSITION]). */
    const val HISTORY = "positions"
    const val LOCATION = "location"
    const val FIX_TIME = "fixTime"
    const val SPEED_KMH = "speedKmh"
    const val COURSE = "course"
    const val ADDRESS = "address"
    const val IGNITION = "ignition"
    const val MOTION = "motion"
    const val POWER_VOLTS = "powerVolts"
    const val VALID = "valid"
    const val ENGINE_BLOCKED = "engineBlocked"
    const val LAST_COMMAND = "lastCommand"
    const val COMMAND_TYPE = "type"
    const val COMMAND_STATUS = "status"
    const val COMMAND_ERROR = "errorCode"
    const val COMMAND_REQUESTED_AT = "requestedAt"
}

internal fun vehicleFromDocument(id: String, data: Map<String, Any?>): Vehicle? {
    val name = data[VehicleFields.NAME] as? String ?: return null
    return Vehicle(
        id = id,
        name = name,
        uniqueId = data[VehicleFields.UNIQUE_ID] as? String ?: id,
        status = (data[VehicleFields.STATUS] as? String).toDeviceStatus(),
        lastUpdate = (data[VehicleFields.LAST_UPDATE] as? Timestamp)?.toInstant(),
        category = data[VehicleFields.CATEGORY] as? String,
        position = (data[VehicleFields.POSITION] as? Map<*, *>)?.toPosition(id),
        engineBlocked = data[VehicleFields.ENGINE_BLOCKED] as? Boolean,
        lastCommand = (data[VehicleFields.LAST_COMMAND] as? Map<*, *>)?.toCommand()
    )
}

internal fun Map<*, *>.toPosition(vehicleId: String): VehiclePosition? {
    val location = this[VehicleFields.LOCATION] as? GeoPoint ?: return null
    val fixTime = (this[VehicleFields.FIX_TIME] as? Timestamp)?.toInstant() ?: return null
    return VehiclePosition(
        vehicleId = vehicleId,
        coordinates = Coordinates(location.latitude, location.longitude),
        fixTime = fixTime,
        speedKmh = (this[VehicleFields.SPEED_KMH] as? Number)?.toDouble() ?: 0.0,
        course = (this[VehicleFields.COURSE] as? Number)?.toDouble() ?: 0.0,
        address = this[VehicleFields.ADDRESS] as? String,
        ignition = this[VehicleFields.IGNITION] as? Boolean,
        motion = this[VehicleFields.MOTION] as? Boolean,
        powerVolts = (this[VehicleFields.POWER_VOLTS] as? Number)?.toDouble(),
        valid = this[VehicleFields.VALID] as? Boolean ?: true
    )
}

private fun Map<*, *>.toCommand(): VehicleCommand? {
    val type = (this[VehicleFields.COMMAND_TYPE] as? String)?.let(::commandTypeOf) ?: return null
    val status = (this[VehicleFields.COMMAND_STATUS] as? String)?.let { wire ->
        CommandStatus.entries.firstOrNull { it.name.equals(wire, ignoreCase = true) }
    } ?: return null
    return VehicleCommand(
        type = type,
        status = status,
        errorCode = this[VehicleFields.COMMAND_ERROR] as? String,
        requestedAt = (this[VehicleFields.COMMAND_REQUESTED_AT] as? Timestamp)?.toInstant()
    )
}

/** Wire names follow Traccar command types. */
internal val CommandType.wireName: String
    get() = when (this) {
        CommandType.ENGINE_STOP -> "engineStop"
        CommandType.ENGINE_RESUME -> "engineResume"
    }

private fun commandTypeOf(wire: String): CommandType? = CommandType.entries.firstOrNull { it.wireName == wire }

internal fun String?.toDeviceStatus(): DeviceStatus = when (this?.lowercase()) {
    "online" -> DeviceStatus.ONLINE
    "offline" -> DeviceStatus.OFFLINE
    else -> DeviceStatus.UNKNOWN
}

private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())

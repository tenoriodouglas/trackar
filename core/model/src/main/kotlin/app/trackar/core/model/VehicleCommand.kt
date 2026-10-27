package app.trackar.core.model

import java.time.Instant

/** Remote actions supported through the tracker (mapped to Traccar command types). */
enum class CommandType { ENGINE_STOP, ENGINE_RESUME }

enum class CommandStatus { PENDING, PROCESSING, SENT, QUEUED, FAILED }

/** Latest command sent to a vehicle, mirrored on the vehicle document by the backend. */
data class VehicleCommand(
    val type: CommandType,
    val status: CommandStatus,
    /** Backend error code when [status] is FAILED (e.g. "unsupported", "device_not_connected"). */
    val errorCode: String?,
    val requestedAt: Instant?
) {
    val inProgress: Boolean get() = status == CommandStatus.PENDING || status == CommandStatus.PROCESSING
}

/** The backend refused a command before sending it to the tracker. */
class CommandRejectedException(val reason: Reason, message: String? = null) : Exception(message) {
    enum class Reason { VEHICLE_MOVING, TOO_SOON, NOT_ALLOWED, UNKNOWN }
}

/** The tracker is already linked to another account. */
class TrackerAlreadyClaimedException : Exception("Tracker already claimed")

/** The tracker identifier (IMEI) is not valid. */
class InvalidTrackerIdException : Exception("Invalid tracker id")

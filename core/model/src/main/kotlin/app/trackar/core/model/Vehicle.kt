package app.trackar.core.model

import java.time.Instant

/** A tracked vehicle backed by an M2M/GPS device. */
data class Vehicle(
    val id: String,
    val name: String,
    val uniqueId: String,
    val status: DeviceStatus,
    val lastUpdate: Instant?,
    val category: String?,
    val position: VehiclePosition?,
    /** Last block state confirmed by the gateway; null when never commanded. */
    val engineBlocked: Boolean? = null,
    val lastCommand: VehicleCommand? = null
)

enum class DeviceStatus { ONLINE, OFFLINE, UNKNOWN }

package app.trackar.core.model

import java.time.Instant

/** Last known GPS fix reported by a device. */
data class VehiclePosition(
    val vehicleId: String,
    val coordinates: Coordinates,
    val fixTime: Instant,
    /** Ground speed in km/h. */
    val speedKmh: Double,
    /** Heading in degrees (0-360, clockwise from north). */
    val course: Double,
    val address: String?,
    val ignition: Boolean?,
    val motion: Boolean?,
    /** Vehicle battery/power voltage, when reported. */
    val powerVolts: Double?,
    val valid: Boolean
)

data class Coordinates(val latitude: Double, val longitude: Double)

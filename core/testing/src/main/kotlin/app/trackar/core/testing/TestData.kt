package app.trackar.core.testing

import app.trackar.core.model.Coordinates
import app.trackar.core.model.DeviceStatus
import app.trackar.core.model.Vehicle
import app.trackar.core.model.VehiclePosition
import java.time.Instant

fun testVehicle(
    id: String = "1",
    name: String = "Vehicle $id",
    status: DeviceStatus = DeviceStatus.ONLINE,
    withPosition: Boolean = true
) = Vehicle(
    id = id,
    name = name,
    uniqueId = "IMEI-$id",
    status = status,
    lastUpdate = Instant.parse("2026-01-01T12:00:00Z"),
    category = "car",
    position = if (withPosition) {
        VehiclePosition(
            vehicleId = id,
            coordinates = Coordinates(-23.55, -46.63),
            fixTime = Instant.parse("2026-01-01T12:00:00Z"),
            speedKmh = 42.0,
            course = 90.0,
            address = "Av. Paulista, São Paulo",
            ignition = true,
            motion = true,
            powerVolts = 12.6,
            valid = true
        )
    } else {
        null
    }
)

package app.trackar.core.model

import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Test

class TripSummaryTest {

    private val t0 = Instant.parse("2026-01-01T08:00:00Z")

    @Test
    fun `empty history yields zeros`() {
        val summary = emptyList<VehiclePosition>().summarize()
        assertEquals(0.0, summary.distanceKm, 0.0)
        assertEquals(0, summary.stops.size)
    }

    @Test
    fun `distance, max speed and moving time are computed`() {
        // ~1.11 km per 0.01 degree of latitude.
        val summary = listOf(
            position(0, lat = 0.00, speed = 50.0),
            position(1, lat = 0.01, speed = 70.0),
            position(2, lat = 0.02, speed = 0.0)
        ).summarize()

        assertEquals(2.22, summary.distanceKm, 0.01)
        assertEquals(70.0, summary.maxSpeedKmh, 0.0)
        assertEquals(2.minutes, summary.movingTime)
    }

    @Test
    fun `stops shorter than five minutes are ignored`() {
        val summary = listOf(
            position(0, speed = 40.0),
            position(1, speed = 0.0),
            position(3, speed = 0.0),
            position(4, speed = 40.0),
            position(5, speed = 0.0),
            position(20, speed = 0.0),
            position(21, speed = 30.0)
        ).summarize()

        assertEquals(1, summary.stops.size)
        assertEquals(16.minutes, summary.stops.single().duration)
    }

    @Test
    fun `trailing stop is closed at the last fix`() {
        val summary = listOf(position(0, speed = 40.0), position(1, speed = 0.0), position(30, speed = 0.0)).summarize()
        assertEquals(29.minutes, summary.stops.single().duration)
    }

    @Test
    fun `invalid fixes are ignored`() {
        val summary = listOf(position(0, speed = 10.0), position(1, speed = 300.0, valid = false)).summarize()
        assertEquals(10.0, summary.maxSpeedKmh, 0.0)
    }

    private fun position(minute: Long, lat: Double = 0.0, speed: Double, valid: Boolean = true) = VehiclePosition(
        vehicleId = "1",
        coordinates = Coordinates(lat, 0.0),
        fixTime = t0.plusSeconds(minute * 60),
        speedKmh = speed,
        course = 0.0,
        address = null,
        ignition = null,
        motion = null,
        powerVolts = null,
        valid = valid
    )
}

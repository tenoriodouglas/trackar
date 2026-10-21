package app.trackar.core.model

import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** Aggregated figures for a set of positions (e.g. one day of history). */
data class TripSummary(val distanceKm: Double, val maxSpeedKmh: Double, val movingTime: Duration, val stops: List<Stop>)

/** A period where the vehicle stayed still for at least [MIN_STOP]. */
data class Stop(val coordinates: Coordinates, val start: Instant, val end: Instant) {
    val duration: Duration get() = (end.toEpochMilli() - start.toEpochMilli()).milliseconds
}

private const val MOVING_THRESHOLD_KMH = 3.0
private val MIN_STOP = 5.minutes
private const val EARTH_RADIUS_KM = 6371.0

/** Summarizes chronologically sorted positions. Invalid GPS fixes are ignored. */
fun List<VehiclePosition>.summarize(): TripSummary {
    val points = filter { it.valid }
    var distanceKm = 0.0
    var moving = ZERO
    val stops = mutableListOf<Stop>()
    var stillSince: VehiclePosition? = null

    points.zipWithNext { a, b ->
        distanceKm += a.coordinates.distanceKmTo(b.coordinates)
        val elapsed = (b.fixTime.toEpochMilli() - a.fixTime.toEpochMilli()).milliseconds
        if (a.speedKmh >= MOVING_THRESHOLD_KMH) moving += elapsed
    }

    for (point in points) {
        val isStill = point.speedKmh < MOVING_THRESHOLD_KMH
        val since = stillSince
        when {
            isStill && since == null -> stillSince = point

            !isStill && since != null -> {
                stops.addIfLongEnough(since, point.fixTime)
                stillSince = null
            }
        }
    }
    stillSince?.let { stops.addIfLongEnough(it, points.last().fixTime) }

    return TripSummary(
        distanceKm = distanceKm,
        maxSpeedKmh = points.maxOfOrNull { it.speedKmh } ?: 0.0,
        movingTime = moving,
        stops = stops
    )
}

private fun MutableList<Stop>.addIfLongEnough(from: VehiclePosition, until: Instant) {
    val stop = Stop(from.coordinates, from.fixTime, until)
    if (stop.duration >= MIN_STOP) add(stop)
}

/** Great-circle distance (haversine). */
fun Coordinates.distanceKmTo(other: Coordinates): Double {
    val dLat = Math.toRadians(other.latitude - latitude)
    val dLon = Math.toRadians(other.longitude - longitude)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
}

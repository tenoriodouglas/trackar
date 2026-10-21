package app.trackar.core.designsystem.component

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import app.trackar.core.designsystem.R
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.time.Duration

/** "5 min ago"-style label, or a placeholder when unknown. */
@Composable
fun Instant?.relativeLabel(now: Long = System.currentTimeMillis()): String = this?.let {
    DateUtils.getRelativeTimeSpanString(it.toEpochMilli(), now, DateUtils.MINUTE_IN_MILLIS).toString()
} ?: stringResource(R.string.core_designsystem_never)

@Composable
fun speedLabel(speedKmh: Double): String = stringResource(R.string.core_designsystem_speed_kmh, speedKmh.roundToInt())

/** Compass point for a heading, e.g. "Northeast". */
@Composable
fun directionLabel(course: Double): String {
    val points = stringArrayResource(R.array.core_designsystem_compass_points)
    val index = (((course % 360 + 360) % 360 + 22.5) / 45).toInt() % points.size
    return points[index]
}

@Composable
fun distanceLabel(km: Double): String = if (km < 1) {
    stringResource(R.string.core_designsystem_distance_m, (km * 1000).roundToInt())
} else {
    stringResource(R.string.core_designsystem_distance_km, km)
}

@Composable
fun durationLabel(duration: Duration): String = duration.toComponents { hours, minutes, _, _ ->
    if (hours > 0) {
        stringResource(R.string.core_designsystem_duration_hm, hours.toInt(), minutes)
    } else {
        stringResource(R.string.core_designsystem_duration_m, minutes)
    }
}

package app.trackar.core.designsystem.component

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.trackar.core.designsystem.R
import java.time.Instant
import kotlin.math.roundToInt

/** "5 min ago"-style label, or a placeholder when unknown. */
@Composable
fun Instant?.relativeLabel(now: Long = System.currentTimeMillis()): String = this?.let {
    DateUtils.getRelativeTimeSpanString(it.toEpochMilli(), now, DateUtils.MINUTE_IN_MILLIS).toString()
} ?: stringResource(R.string.core_designsystem_never)

@Composable
fun speedLabel(speedKmh: Double): String = stringResource(R.string.core_designsystem_speed_kmh, speedKmh.roundToInt())

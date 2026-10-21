package app.trackar.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trackar.core.designsystem.component.MapRoundButton
import app.trackar.core.designsystem.component.VehicleMarkerIcon
import app.trackar.core.designsystem.component.distanceLabel
import app.trackar.core.designsystem.component.durationLabel
import app.trackar.core.designsystem.component.speedLabel
import app.trackar.core.designsystem.theme.MapColors
import app.trackar.core.model.Coordinates
import app.trackar.core.model.Stop
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DEFAULT_CAMERA = CameraPosition.fromLatLngZoom(LatLng(-14.235, -51.925), 4f)
private const val FIT_PADDING_PX = 140

@Composable
fun HistoryRoute(
    vehicleId: String,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel<HistoryViewModel, HistoryViewModel.Factory>(
        key = "history-$vehicleId",
        creationCallback = { it.create(vehicleId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onBack = onBack,
        onPreviousDay = viewModel::previousDay,
        onNextDay = viewModel::nextDay,
        onSelectDate = viewModel::selectDate,
        onSeek = viewModel::seekTo,
        onTogglePlayback = viewModel::togglePlayback,
        onRetry = viewModel::retry
    )
}

@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSeek: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onRetry: () -> Unit
) {
    val cameraState = rememberCameraPositionState { position = DEFAULT_CAMERA }
    val route = remember(uiState.positions) {
        uiState.positions.map { LatLng(it.coordinates.latitude, it.coordinates.longitude) }
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Frame the whole route whenever a new day is loaded.
    LaunchedEffect(route) {
        when (route.size) {
            0 -> Unit

            1 -> cameraState.animate(CameraUpdateFactory.newLatLngZoom(route.first(), 16f))

            else -> cameraState.animate(
                CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds.builder().apply {
                        route.forEach(::include)
                    }.build(),
                    FIT_PADDING_PX
                )
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = false)
        ) {
            if (route.size >= 2) {
                // Outline + fill gives the thick, high-contrast route look.
                Polyline(
                    points = route,
                    color = MapColors.RouteOutline,
                    width = 22f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
                Polyline(
                    points = route,
                    color = MapColors.Route,
                    width = 14f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
                RouteEndpoint(route.first(), MapColors.Start, Icons.Filled.Flag, "start")
                RouteEndpoint(route.last(), MapColors.End, Icons.Filled.Flag, "end")
            }
            uiState.summary?.stops?.forEachIndexed { index, stop -> StopMarker(stop, index) }
            uiState.current?.let { position ->
                val markerState = rememberUpdatedMarkerState(position.coordinates.toLatLng())
                MarkerComposable(
                    "cursor",
                    position.course.toInt() / 5,
                    state = markerState,
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 2f
                ) {
                    VehicleMarkerIcon(color = MaterialTheme.colorScheme.primary, course = position.course.toFloat())
                }
            }
        }

        DayBar(
            uiState = uiState,
            onBack = onBack,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onOpenCalendar = { showDatePicker = true },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)
        )

        BottomPanel(
            uiState = uiState,
            onSeek = onSeek,
            onTogglePlayback = onTogglePlayback,
            onRetry = onRetry,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDatePicker) {
        HistoryDatePicker(
            selected = uiState.date,
            today = uiState.today,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                showDatePicker = false
                onSelectDate(it)
            }
        )
    }
}

@Composable
private fun RouteEndpoint(point: LatLng, color: Color, icon: ImageVector, key: String) {
    MarkerComposable(
        key,
        color.toArgb(),
        state = rememberUpdatedMarkerState(point),
        anchor = Offset(0.5f, 0.5f),
        zIndex = 1f
    ) {
        Box(
            Modifier.size(32.dp).background(Color.White, CircleShape).padding(3.dp).background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StopMarker(stop: Stop, index: Int) {
    MarkerComposable(
        "stop-$index",
        state = rememberUpdatedMarkerState(stop.coordinates.toLatLng()),
        anchor = Offset(0.5f, 0.5f),
        title = stringResource(R.string.feature_history_stop_title, durationLabel(stop.duration))
    ) {
        Box(
            Modifier.size(
                28.dp
            ).background(Color.White, CircleShape).padding(3.dp).background(MapColors.Stop, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.LocalParking,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DayBar(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MapRoundButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_history_back), onBack)
        Spacer(Modifier.width(10.dp))
        Surface(shape = CircleShape, shadowElevation = 6.dp, modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                IconButton(onClick = onPreviousDay) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(R.string.feature_history_previous_day)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenCalendar).padding(vertical = 6.dp)
                ) {
                    Text(dayLabel(uiState.date, uiState.today), style = MaterialTheme.typography.titleMedium)
                    if (uiState.vehicleName.isNotEmpty()) {
                        Text(
                            uiState.vehicleName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onOpenCalendar) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = stringResource(R.string.feature_history_pick_date)
                    )
                }
                IconButton(onClick = onNextDay, enabled = uiState.canGoForward) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.feature_history_next_day)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomPanel(
    uiState: HistoryUiState,
    onSeek: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp
    ) {
        Column(Modifier.navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                uiState.isLoading -> {
                    Text(stringResource(R.string.feature_history_loading), style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                uiState.hasError -> {
                    Text(stringResource(R.string.feature_history_error), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onRetry) { Text(stringResource(R.string.feature_history_retry)) }
                }

                uiState.isEmpty -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.feature_history_empty_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.feature_history_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    uiState.summary?.let { summary ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(
                                Icons.Filled.Route,
                                distanceLabel(summary.distanceKm),
                                stringResource(R.string.feature_history_distance),
                                Modifier.weight(1f)
                            )
                            StatTile(
                                Icons.Filled.Speed,
                                speedLabel(summary.maxSpeedKmh),
                                stringResource(R.string.feature_history_max_speed),
                                Modifier.weight(1f)
                            )
                            StatTile(
                                Icons.Filled.Timer,
                                durationLabel(summary.movingTime),
                                stringResource(R.string.feature_history_moving),
                                Modifier.weight(1f)
                            )
                            StatTile(
                                Icons.Filled.LocalParking,
                                summary.stops.size.toString(),
                                stringResource(R.string.feature_history_stops),
                                Modifier.weight(1f)
                            )
                        }
                    }
                    PlaybackBar(uiState, onSeek, onTogglePlayback)
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PlaybackBar(uiState: HistoryUiState, onSeek: (Int) -> Unit, onTogglePlayback: () -> Unit) {
    val current = uiState.current ?: return
    val locale = LocalLocale.current.platformLocale
    val timeFormatter =
        remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale).withZone(ZoneId.systemDefault()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FloatingActionButton(
            onClick = onTogglePlayback,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (uiState.isPlaying) R.string.feature_history_pause else R.string.feature_history_play
                )
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(timeFormatter.format(current.fixTime), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    speedLabel(current.speedKmh),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = uiState.playbackIndex.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..(uiState.positions.size - 1).coerceAtLeast(1).toFloat()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDatePicker(
    selected: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    // DatePicker works with UTC midnight millis.
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int) = year <= today.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text(stringResource(R.string.feature_history_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_history_cancel)) } }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String {
    val locale = LocalLocale.current.platformLocale
    return when (date) {
        today -> stringResource(R.string.feature_history_today)
        today.minusDays(1) -> stringResource(R.string.feature_history_yesterday)
        else -> remember(date, locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale).format(date) }
    }
}

private fun Coordinates.toLatLng() = LatLng(latitude, longitude)

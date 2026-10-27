package app.trackar.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trackar.core.designsystem.component.LabeledVehicleMarker
import app.trackar.core.designsystem.component.MapRoundButton
import app.trackar.core.designsystem.component.SpeedBubble
import app.trackar.core.designsystem.component.StatusIndicator
import app.trackar.core.designsystem.component.VehicleMarkerIcon
import app.trackar.core.designsystem.component.color
import app.trackar.core.designsystem.component.directionLabel
import app.trackar.core.designsystem.component.relativeLabel
import app.trackar.core.model.ConnectionState
import app.trackar.core.model.Coordinates
import app.trackar.core.model.Vehicle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch

private val DEFAULT_CAMERA = CameraPosition.fromLatLngZoom(LatLng(-14.235, -51.925), 4f) // Brazil
private const val FIT_PADDING_PX = 180
private const val FOLLOW_ZOOM = 16f
private const val MOVE_ANIMATION_MS = 1_200
private const val MAX_SUGGESTIONS = 5

@Composable
fun MapRoute(
    onOpenDetails: (String) -> Unit,
    onOpenHistory: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(
        uiState = uiState,
        onVehicleSelected = viewModel::onVehicleSelected,
        onOpenDetails = onOpenDetails,
        onOpenHistory = onOpenHistory
    )
}

@Composable
internal fun MapScreen(
    uiState: MapUiState,
    onVehicleSelected: (String?) -> Unit,
    onOpenDetails: (String) -> Unit,
    onOpenHistory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraState = rememberCameraPositionState { position = DEFAULT_CAMERA }
    val scope = rememberCoroutineScope()
    var hasFitted by rememberSaveable { mutableStateOf(false) }
    var following by rememberSaveable { mutableStateOf(false) }
    val ready = uiState as? MapUiState.Ready
    val selected = ready?.selected

    // Frame the fleet once, on the first data load.
    LaunchedEffect(ready?.vehicles?.isNotEmpty()) {
        if (!hasFitted && ready != null && ready.vehicles.isNotEmpty()) {
            cameraState.fitAll(ready.vehicles)
            hasFitted = true
        }
    }

    // Follow mode keeps the selected vehicle centered as live updates arrive.
    val followTarget = selected?.position?.coordinates
    LaunchedEffect(followTarget, following) {
        if (following && followTarget != null) {
            cameraState.animate(
                CameraUpdateFactory.newLatLngZoom(followTarget.toLatLng(), FOLLOW_ZOOM),
                MOVE_ANIMATION_MS
            )
        }
    }

    // Any manual pan stops following, like turn-by-turn apps do.
    LaunchedEffect(cameraState.isMoving) {
        if (cameraState.isMoving &&
            cameraState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        ) {
            following = false
        }
    }

    Box(modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = false),
            onMapClick = { onVehicleSelected(null) }
        ) {
            ready?.vehicles?.forEach { vehicle ->
                VehicleMarker(
                    vehicle = vehicle,
                    selected = vehicle.id == selected?.id,
                    onClick = {
                        onVehicleSelected(vehicle.id)
                        following = true
                    }
                )
            }
        }

        if (uiState is MapUiState.Loading) CircularProgressIndicator(Modifier.align(Alignment.Center))

        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)) {
            SearchPill(
                vehicles = ready?.vehicles.orEmpty(),
                onPick = { vehicle ->
                    onVehicleSelected(vehicle.id)
                    following = true
                }
            )
            ready?.connection?.takeIf {
                it != ConnectionState.CONNECTED
            }?.let { ConnectionBanner(it, Modifier.align(Alignment.CenterHorizontally)) }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(
                end = 16.dp,
                bottom = if (selected !=
                    null
                ) {
                    260.dp
                } else {
                    88.dp
                }
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (following) selected?.position?.let { SpeedBubble(it.speedKmh) }
            if (selected != null) {
                MapRoundButton(
                    icon = Icons.Filled.NearMe,
                    contentDescription = stringResource(R.string.feature_map_follow),
                    onClick = { following = !following },
                    highlighted = following
                )
            }
            MapRoundButton(
                icon = Icons.Filled.ZoomOutMap,
                contentDescription = stringResource(R.string.feature_map_show_all),
                onClick = {
                    following = false
                    onVehicleSelected(null)
                    ready?.vehicles?.let { scope.launch { cameraState.fitAll(it) } }
                }
            )
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selected?.let { vehicle ->
                VehicleSheet(
                    vehicle = vehicle,
                    onClose = {
                        following = false
                        onVehicleSelected(null)
                    },
                    onOpenHistory = { onOpenHistory(vehicle.id) },
                    onOpenDetails = { onOpenDetails(vehicle.id) }
                )
            }
        }

        if (selected == null && ready != null) {
            FleetPill(ready, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

/** Marker that glides between live positions instead of jumping. */
@Composable
private fun VehicleMarker(vehicle: Vehicle, selected: Boolean, onClick: () -> Unit) {
    val position = vehicle.position ?: return
    val target = position.coordinates.toLatLng()
    var shown by remember(vehicle.id) { mutableStateOf(target) }
    LaunchedEffect(target) {
        val start = shown
        animate(0f, 1f, animationSpec = tween(MOVE_ANIMATION_MS)) { fraction, _ ->
            shown = start.lerp(target, fraction)
        }
    }
    val course = position.course.toFloat()
    MarkerComposable(
        vehicle.id,
        vehicle.name,
        vehicle.status,
        selected,
        (course / 5).toInt(), // Re-render the bitmap only on meaningful heading changes.
        state = rememberUpdatedMarkerState(shown),
        anchor = Offset(0.5f, 0.7f),
        zIndex = if (selected) 2f else 1f,
        onClick = {
            onClick()
            true
        }
    ) {
        LabeledVehicleMarker(name = vehicle.name, color = vehicle.status.color, course = course, selected = selected)
    }
}

@Composable
private fun SearchPill(vehicles: List<Vehicle>, onPick: (Vehicle) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val suggestions = remember(query, vehicles) {
        if (query.isBlank()) {
            emptyList()
        } else {
            vehicles.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.uniqueId.contains(query, ignoreCase = true)
            }
                .take(MAX_SUGGESTIONS)
        }
    }
    Surface(shape = RoundedCornerShape(28.dp), shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.feature_map_search_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.merge(
                            TextStyle(color = MaterialTheme.colorScheme.onSurface)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_map_clear))
                    }
                }
            }
            suggestions.forEach { vehicle ->
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().clickable {
                        query = ""
                        onPick(vehicle)
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VehicleMarkerIcon(vehicle.status.color, vehicle.position?.course?.toFloat(), size = 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(vehicle.name, style = MaterialTheme.typography.titleSmall)
                        Text(vehicle.lastUpdate.relativeLabel(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionBanner(connection: ConnectionState, modifier: Modifier = Modifier) {
    val label = if (connection ==
        ConnectionState.CONNECTING
    ) {
        R.string.feature_map_connecting
    } else {
        R.string.feature_map_offline
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        shadowElevation = 4.dp,
        modifier = modifier.padding(top = 8.dp)
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FleetPill(state: MapUiState.Ready, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, shadowElevation = 6.dp, modifier = modifier) {
        Text(
            text = if (state.vehicles.isEmpty()) {
                stringResource(R.string.feature_map_no_vehicles)
            } else {
                stringResource(R.string.feature_map_fleet_summary, state.vehicles.size, state.onlineCount)
            },
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun VehicleSheet(vehicle: Vehicle, onClose: () -> Unit, onOpenHistory: () -> Unit, onOpenDetails: () -> Unit) {
    val position = vehicle.position ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 16.dp
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpeedBubble(position.speedKmh)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(vehicle.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    StatusIndicator(vehicle.status)
                    Text(
                        stringResource(
                            R.string.feature_map_heading,
                            directionLabel(position.course),
                            position.fixTime.relativeLabel()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_map_close))
                }
            }
            position.address?.let { address ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(address, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (vehicle.engineBlocked == true) {
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text(stringResource(R.string.feature_map_blocked)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
                position.ignition?.let { on ->
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = {
                            Text(
                                stringResource(
                                    if (on) R.string.feature_map_ignition_on else R.string.feature_map_ignition_off
                                )
                            )
                        }
                    )
                }
                position.powerVolts?.let { volts ->
                    AssistChip(
                        onClick = {},
                        leadingIcon = {
                            Icon(Icons.Filled.Battery5Bar, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text(stringResource(R.string.feature_map_volts, volts)) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenHistory, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feature_map_history))
                }
                OutlinedButton(onClick = onOpenDetails, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feature_map_details))
                }
            }
        }
    }
}

private suspend fun CameraPositionState.fitAll(vehicles: List<Vehicle>) {
    val points = vehicles.mapNotNull { it.position?.coordinates?.toLatLng() }
    when (points.size) {
        0 -> Unit

        1 -> animate(CameraUpdateFactory.newLatLngZoom(points.first(), FOLLOW_ZOOM))

        else -> animate(
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.builder().apply {
                    points.forEach(::include)
                }.build(),
                FIT_PADDING_PX
            )
        )
    }
}

private fun Coordinates.toLatLng() = LatLng(latitude, longitude)

private fun LatLng.lerp(to: LatLng, fraction: Float) = LatLng(
    latitude + (to.latitude - latitude) * fraction,
    longitude + (to.longitude - longitude) * fraction
)

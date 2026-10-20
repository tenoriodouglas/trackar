package app.trackar.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trackar.core.designsystem.component.StatusIndicator
import app.trackar.core.designsystem.component.relativeLabel
import app.trackar.core.designsystem.component.speedLabel
import app.trackar.core.model.ConnectionState
import app.trackar.core.model.DeviceStatus
import app.trackar.core.model.Vehicle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

private val DEFAULT_CAMERA = CameraPosition.fromLatLngZoom(LatLng(-14.235, -51.925), 4f) // Brazil
private const val FIT_PADDING_PX = 160
private const val SINGLE_VEHICLE_ZOOM = 15f

@Composable
fun MapRoute(onOpenDetails: (String) -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(uiState = uiState, onVehicleSelected = viewModel::onVehicleSelected, onOpenDetails = onOpenDetails)
}

@Composable
internal fun MapScreen(
    uiState: MapUiState,
    onVehicleSelected: (String?) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraState = rememberCameraPositionState { position = DEFAULT_CAMERA }
    var hasFitted by rememberSaveable { mutableStateOf(false) }

    // Frame the fleet once, on the first data load.
    LaunchedEffect(uiState) {
        val vehicles = (uiState as? MapUiState.Ready)?.vehicles.orEmpty()
        if (hasFitted || vehicles.isEmpty()) return@LaunchedEffect
        val points = vehicles.mapNotNull { it.position?.coordinates?.let { c -> LatLng(c.latitude, c.longitude) } }
        val update = if (points.size == 1) {
            CameraUpdateFactory.newLatLngZoom(points.first(), SINGLE_VEHICLE_ZOOM)
        } else {
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.builder().apply {
                    points.forEach(::include)
                }.build(),
                FIT_PADDING_PX
            )
        }
        cameraState.animate(update)
        hasFitted = true
    }

    Box(modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
            onMapClick = { onVehicleSelected(null) }
        ) {
            (uiState as? MapUiState.Ready)?.vehicles?.forEach { vehicle ->
                VehicleMarker(vehicle = vehicle, onClick = { onVehicleSelected(vehicle.id) })
            }
        }

        when (uiState) {
            MapUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            is MapUiState.Ready -> {
                ConnectionChip(
                    uiState.connection,
                    Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(8.dp)
                )
                uiState.selected?.let { vehicle ->
                    SelectedVehicleCard(
                        vehicle = vehicle,
                        onOpenDetails = { onOpenDetails(vehicle.id) },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleMarker(vehicle: Vehicle, onClick: () -> Unit) {
    val position = vehicle.position ?: return
    val markerState = rememberUpdatedMarkerState(LatLng(position.coordinates.latitude, position.coordinates.longitude))
    Marker(
        state = markerState,
        title = vehicle.name,
        rotation = position.course.toFloat(),
        flat = true,
        icon = BitmapDescriptorFactory.defaultMarker(vehicle.status.markerHue()),
        onClick = {
            onClick()
            false
        }
    )
}

private fun DeviceStatus.markerHue(): Float = when (this) {
    DeviceStatus.ONLINE -> BitmapDescriptorFactory.HUE_GREEN
    DeviceStatus.OFFLINE -> BitmapDescriptorFactory.HUE_RED
    DeviceStatus.UNKNOWN -> BitmapDescriptorFactory.HUE_YELLOW
}

@Composable
private fun ConnectionChip(connection: ConnectionState, modifier: Modifier = Modifier) {
    if (connection == ConnectionState.CONNECTED) return
    val label = when (connection) {
        ConnectionState.CONNECTING -> R.string.feature_map_connecting
        else -> R.string.feature_map_offline
    }
    AssistChip(onClick = {}, label = { Text(stringResource(label)) }, modifier = modifier)
}

@Composable
private fun SelectedVehicleCard(vehicle: Vehicle, onOpenDetails: () -> Unit, modifier: Modifier = Modifier) {
    val position = vehicle.position ?: return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(vehicle.name, style = MaterialTheme.typography.titleMedium)
                StatusIndicator(vehicle.status)
            }
            Text(
                stringResource(
                    R.string.feature_map_speed_and_update,
                    speedLabel(position.speedKmh),
                    position.fixTime.relativeLabel()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            position.address?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onOpenDetails, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.feature_map_details))
            }
        }
    }
}

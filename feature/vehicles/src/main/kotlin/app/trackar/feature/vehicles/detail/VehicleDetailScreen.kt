package app.trackar.feature.vehicles.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trackar.core.designsystem.component.StatusIndicator
import app.trackar.core.designsystem.component.relativeLabel
import app.trackar.core.designsystem.component.speedLabel
import app.trackar.core.model.Coordinates
import app.trackar.core.model.Vehicle
import app.trackar.feature.vehicles.R
import java.util.Locale

@Composable
fun VehicleDetailRoute(
    vehicleId: String,
    onBack: () -> Unit,
    viewModel: VehicleDetailViewModel = hiltViewModel<VehicleDetailViewModel, VehicleDetailViewModel.Factory>(
        key = vehicleId,
        creationCallback = { it.create(vehicleId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VehicleDetailScreen(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleDetailScreen(uiState: VehicleDetailUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text((uiState as? VehicleDetailUiState.Ready)?.vehicle?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_vehicles_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                VehicleDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                VehicleDetailUiState.NotFound -> Text(
                    stringResource(R.string.feature_vehicles_not_found),
                    modifier = Modifier.align(Alignment.Center)
                )

                is VehicleDetailUiState.Ready -> VehicleDetails(uiState.vehicle)
            }
        }
    }
}

@Composable
private fun VehicleDetails(vehicle: Vehicle) {
    val position = vehicle.position
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        DetailRow(R.string.feature_vehicles_status) { StatusIndicator(vehicle.status) }
        DetailRow(R.string.feature_vehicles_identifier, vehicle.uniqueId)
        DetailRow(R.string.feature_vehicles_last_update, vehicle.lastUpdate.relativeLabel())
        if (position != null) {
            DetailRow(R.string.feature_vehicles_speed, speedLabel(position.speedKmh))
            position.ignition?.let { DetailRow(R.string.feature_vehicles_ignition, stringResource(it.onOff())) }
            position.powerVolts?.let {
                DetailRow(R.string.feature_vehicles_power, stringResource(R.string.feature_vehicles_volts, it))
            }
            DetailRow(R.string.feature_vehicles_address, position.address ?: position.coordinates.format())
            DetailRow(R.string.feature_vehicles_fix_time, position.fixTime.relativeLabel())
            NavigateButton(position.coordinates)
        } else {
            DetailRow(R.string.feature_vehicles_address, stringResource(R.string.feature_vehicles_no_position))
        }
    }
}

@Composable
private fun DetailRow(label: Int, value: String) = DetailRow(label) { Text(value) }

@Composable
private fun DetailRow(label: Int, value: @Composable () -> Unit) {
    ListItem(overlineContent = { Text(stringResource(label)) }, headlineContent = value)
}

/** Hands navigation off to any installed maps app via a standard geo intent. */
@Composable
private fun NavigateButton(coordinates: Coordinates) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Button(
            onClick = {
                val uri = "geo:0,0?q=${coordinates.latitude},${coordinates.longitude}".toUri()
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: ActivityNotFoundException) {
                    // No maps app installed; nothing sensible to fall back to.
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Directions, contentDescription = null)
            Text(stringResource(R.string.feature_vehicles_navigate), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun Boolean.onOff(): Int = if (this) R.string.feature_vehicles_on else R.string.feature_vehicles_off

private fun Coordinates.format(): String = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

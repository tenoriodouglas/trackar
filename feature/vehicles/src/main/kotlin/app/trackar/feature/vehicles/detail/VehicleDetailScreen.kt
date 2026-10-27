package app.trackar.feature.vehicles.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import app.trackar.core.model.CommandType
import app.trackar.core.model.Coordinates
import app.trackar.core.model.Vehicle
import app.trackar.feature.vehicles.R
import java.util.Locale

@Composable
fun VehicleDetailRoute(
    vehicleId: String,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: VehicleDetailViewModel = hiltViewModel<VehicleDetailViewModel, VehicleDetailViewModel.Factory>(
        key = vehicleId,
        creationCallback = { it.create(vehicleId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    LaunchedEffect(actionState.released) { if (actionState.released) onBack() }
    VehicleDetailScreen(
        uiState = uiState,
        actionState = actionState,
        onBack = onBack,
        onOpenHistory = onOpenHistory,
        onCommand = viewModel::sendCommand,
        onRelease = viewModel::release,
        onDismissError = viewModel::dismissError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleDetailScreen(
    uiState: VehicleDetailUiState,
    actionState: DetailActionState,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onCommand: (CommandType) -> Unit,
    onRelease: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmRelease by rememberSaveable { mutableStateOf(false) }
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
                },
                actions = {
                    if (uiState is VehicleDetailUiState.Ready) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.feature_vehicles_more)
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.feature_vehicles_release)) },
                                leadingIcon = { Icon(Icons.Filled.LinkOff, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    confirmRelease = true
                                }
                            )
                        }
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

                is VehicleDetailUiState.Ready -> VehicleDetails(
                    uiState.vehicle,
                    actionState.sending,
                    onOpenHistory,
                    onCommand
                )
            }
        }
    }

    actionState.error?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.feature_vehicles_action_failed)) },
            text = { Text(stringResource(error.message())) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text(stringResource(R.string.feature_vehicles_ok)) }
            }
        )
    }

    if (confirmRelease) {
        AlertDialog(
            onDismissRequest = { confirmRelease = false },
            title = { Text(stringResource(R.string.feature_vehicles_release)) },
            text = { Text(stringResource(R.string.feature_vehicles_release_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRelease = false
                    onRelease()
                }) { Text(stringResource(R.string.feature_vehicles_release_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmRelease = false
                }) { Text(stringResource(R.string.feature_vehicles_cancel)) }
            }
        )
    }
}

private fun DetailError.message(): Int = when (this) {
    DetailError.TOO_SOON -> R.string.feature_vehicles_error_too_soon
    DetailError.NOT_ALLOWED -> R.string.feature_vehicles_error_not_allowed
    DetailError.NETWORK -> R.string.feature_vehicles_error_network
    DetailError.UNKNOWN -> R.string.feature_vehicles_error_unknown
}

@Composable
private fun VehicleDetails(
    vehicle: Vehicle,
    sending: Boolean,
    onOpenHistory: () -> Unit,
    onCommand: (CommandType) -> Unit
) {
    val position = vehicle.position
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        EngineBlockCard(vehicle, sending, onCommand, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
            ActionButtons(position.coordinates, onOpenHistory)
        } else {
            DetailRow(R.string.feature_vehicles_address, stringResource(R.string.feature_vehicles_no_position))
            HistoryButton(onOpenHistory)
        }
    }
}

@Composable
private fun DetailRow(label: Int, value: String) = DetailRow(label) { Text(value) }

@Composable
private fun DetailRow(label: Int, value: @Composable () -> Unit) {
    ListItem(overlineContent = { Text(stringResource(label)) }, headlineContent = value)
}

@Composable
private fun HistoryButton(onOpenHistory: () -> Unit) {
    Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)) {
        Icon(Icons.Filled.History, contentDescription = null)
        Text(stringResource(R.string.feature_vehicles_history), modifier = Modifier.padding(start = 8.dp))
    }
}

/** History plus hand-off to any installed maps app via a standard geo intent. */
@Composable
private fun ActionButtons(coordinates: Coordinates, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.History, contentDescription = null)
            Text(stringResource(R.string.feature_vehicles_history), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(
            onClick = {
                val uri = "geo:0,0?q=${coordinates.latitude},${coordinates.longitude}".toUri()
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: ActivityNotFoundException) {
                    // No maps app installed; nothing sensible to fall back to.
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Filled.Directions, contentDescription = null)
            Text(stringResource(R.string.feature_vehicles_navigate), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun Boolean.onOff(): Int = if (this) R.string.feature_vehicles_on else R.string.feature_vehicles_off

private fun Coordinates.format(): String = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

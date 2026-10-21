package app.trackar.feature.vehicles.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trackar.core.designsystem.component.StatusIndicator
import app.trackar.core.designsystem.component.VehicleMarkerIcon
import app.trackar.core.designsystem.component.color
import app.trackar.core.designsystem.component.relativeLabel
import app.trackar.core.designsystem.component.speedLabel
import app.trackar.core.designsystem.theme.TrackarTheme
import app.trackar.core.model.DeviceStatus
import app.trackar.core.model.Vehicle
import app.trackar.feature.vehicles.R

@Composable
fun VehicleListRoute(onVehicleClick: (String) -> Unit, viewModel: VehicleListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    VehicleListScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onQueryChange = viewModel::onQueryChange,
        onRefresh = viewModel::refresh,
        onLogout = viewModel::logout,
        onVehicleClick = onVehicleClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleListScreen(
    uiState: VehicleListUiState,
    isRefreshing: Boolean,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onVehicleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_vehicles_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.feature_vehicles_logout)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                VehicleListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is VehicleListUiState.Ready -> {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.feature_vehicles_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.vehicles.isEmpty()) {
                            Text(
                                stringResource(R.string.feature_vehicles_empty),
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(uiState.vehicles, key = Vehicle::id) { vehicle ->
                                VehicleRow(vehicle, onClick = { onVehicleClick(vehicle.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleRow(vehicle: Vehicle, onClick: () -> Unit) {
    val position = vehicle.position
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            VehicleMarkerIcon(color = vehicle.status.color, course = position?.course?.toFloat(), size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(vehicle.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    position?.address ?: stringResource(R.string.feature_vehicles_no_position),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                StatusIndicator(vehicle.status)
            }
            Column(horizontalAlignment = Alignment.End) {
                position?.let {
                    Text(
                        speedLabel(it.speedKmh),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    (position?.fixTime ?: vehicle.lastUpdate).relativeLabel(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Preview
@Composable
private fun VehicleListPreview() {
    TrackarTheme {
        VehicleListScreen(
            uiState = VehicleListUiState.Ready(
                vehicles = listOf(
                    Vehicle("1", "Hilux ABC1D23", "123", DeviceStatus.ONLINE, null, null, null),
                    Vehicle("2", "Strada XYZ9K87", "456", DeviceStatus.OFFLINE, null, null, null)
                ),
                query = ""
            ),
            isRefreshing = false,
            onQueryChange = {},
            onRefresh = {},
            onLogout = {},
            onVehicleClick = {}
        )
    }
}

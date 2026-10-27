package app.trackar.feature.vehicles.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trackar.core.designsystem.component.relativeLabel
import app.trackar.core.designsystem.theme.StatusColors
import app.trackar.core.model.CommandStatus
import app.trackar.core.model.CommandType
import app.trackar.core.model.Vehicle
import app.trackar.core.model.VehicleCommand
import app.trackar.feature.vehicles.R
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Remote fuel/ignition cut. Blocking is immediate at any speed, so the confirmation explicitly warns
 * when the vehicle is moving.
 */
@Composable
internal fun EngineBlockCard(
    vehicle: Vehicle,
    sending: Boolean,
    onCommand: (CommandType) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirming by rememberSaveable { mutableStateOf<CommandType?>(null) }
    val blocked = vehicle.engineBlocked == true
    val busy = sending || vehicle.lastCommand?.inProgress == true

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blocked) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (blocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = if (blocked) MaterialTheme.colorScheme.error else StatusColors.Online
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(
                        when (vehicle.engineBlocked) {
                            true -> R.string.feature_vehicles_engine_blocked
                            false -> R.string.feature_vehicles_engine_released
                            null -> R.string.feature_vehicles_engine_unknown
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            vehicle.lastCommand?.let { CommandStatusLine(it) }

            val nextCommand = if (blocked) CommandType.ENGINE_RESUME else CommandType.ENGINE_STOP
            Button(
                onClick = { confirming = nextCommand },
                enabled = !busy,
                colors = if (blocked) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(if (blocked) Icons.Filled.LockOpen else Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (blocked) R.string.feature_vehicles_unblock else R.string.feature_vehicles_block
                        )
                    )
                }
            }
        }
    }

    confirming?.let { type ->
        ConfirmCommandDialog(
            type = type,
            vehicleName = vehicle.name,
            movingSpeedKmh = vehicle.movingSpeedKmh(),
            onConfirm = {
                confirming = null
                onCommand(type)
            },
            onDismiss = { confirming = null }
        )
    }
}

@Composable
private fun CommandStatusLine(command: VehicleCommand) {
    val text = when (command.status) {
        CommandStatus.PENDING, CommandStatus.PROCESSING -> stringResource(R.string.feature_vehicles_command_sending)

        CommandStatus.SENT -> stringResource(R.string.feature_vehicles_command_sent)

        CommandStatus.QUEUED -> stringResource(R.string.feature_vehicles_command_queued)

        CommandStatus.FAILED -> stringResource(
            when (command.errorCode) {
                "unsupported" -> R.string.feature_vehicles_command_unsupported
                "device_not_connected" -> R.string.feature_vehicles_command_not_connected
                "expired" -> R.string.feature_vehicles_command_expired
                else -> R.string.feature_vehicles_command_failed
            }
        )
    }
    Text(
        "$text · ${command.requestedAt.relativeLabel()}",
        style = MaterialTheme.typography.bodyMedium,
        color = if (command.status == CommandStatus.FAILED) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun ConfirmCommandDialog(
    type: CommandType,
    vehicleName: String,
    movingSpeedKmh: Int?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val blocking = type == CommandType.ENGINE_STOP
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (blocking) Icons.Filled.Warning else Icons.Filled.LockOpen, contentDescription = null) },
        title = {
            Text(
                stringResource(
                    if (blocking) R.string.feature_vehicles_block_title else R.string.feature_vehicles_unblock_title,
                    vehicleName
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (blocking && movingSpeedKmh != null) {
                    Text(
                        stringResource(R.string.feature_vehicles_block_moving_warning, movingSpeedKmh),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                val message = if (blocking) {
                    R.string.feature_vehicles_block_warning
                } else {
                    R.string.feature_vehicles_unblock_message
                }
                Text(stringResource(message))
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (blocking) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    stringResource(if (blocking) R.string.feature_vehicles_block else R.string.feature_vehicles_unblock)
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_vehicles_cancel)) } }
    )
}

/** Current speed when the last fix is recent and shows movement; null when stopped or stale. */
private fun Vehicle.movingSpeedKmh(now: Instant = Instant.now()): Int? {
    val position = position ?: return null
    val fresh = Duration.between(position.fixTime, now) < MOVING_FRESHNESS
    return position.speedKmh.roundToInt().takeIf { fresh && it >= MOVING_THRESHOLD_KMH }
}

private val MOVING_FRESHNESS: Duration = Duration.ofMinutes(10)
private const val MOVING_THRESHOLD_KMH = 3

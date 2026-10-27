package app.trackar.feature.vehicles.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.trackar.feature.vehicles.R

/** Links a tracker to the account by its IMEI (printed on the device label). */
@Composable
internal fun AddTrackerDialog(
    state: AddTrackerState,
    onSubmit: (imei: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var imei by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val busy = state is AddTrackerState.Saving

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Filled.AddLocationAlt, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_vehicles_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.feature_vehicles_add_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = imei,
                    onValueChange = { value -> imei = value.filter(Char::isLetterOrDigit).take(MAX_ID_LENGTH) },
                    label = { Text(stringResource(R.string.feature_vehicles_add_imei)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.feature_vehicles_add_name)) },
                    placeholder = { Text("Hilux ABC1D23") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                (state as? AddTrackerState.Error)?.let {
                    Text(
                        stringResource(
                            when (it.reason) {
                                AddTrackerError.ALREADY_CLAIMED -> R.string.feature_vehicles_add_error_claimed
                                AddTrackerError.INVALID_ID -> R.string.feature_vehicles_add_error_invalid
                                AddTrackerError.UNKNOWN -> R.string.feature_vehicles_error_unknown
                            }
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(imei, name) }, enabled = !busy && imei.length >= MIN_ID_LENGTH) {
                if (busy) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.feature_vehicles_add_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.feature_vehicles_cancel)) }
        }
    )
}

private const val MIN_ID_LENGTH = 4
private const val MAX_ID_LENGTH = 32
private const val MAX_NAME_LENGTH = 40

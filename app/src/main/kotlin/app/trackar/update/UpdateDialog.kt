package app.trackar.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trackar.R

@Composable
fun UpdateGate(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    when (val current = state) {
        UpdateState.Idle -> Unit

        is UpdateState.NeedsPermission -> UpdateAlert(
            current.manifest,
            message = stringResource(R.string.update_permission_message),
            confirm = stringResource(R.string.update_allow) to viewModel::openPermissionSettings,
            onDismiss = viewModel::dismiss
        )

        is UpdateState.Downloading -> UpdateAlert(
            current.manifest,
            message = stringResource(R.string.update_downloading),
            progress = current.progress,
            onDismiss = viewModel::dismiss
        )

        is UpdateState.Failed -> UpdateAlert(
            current.manifest,
            message = stringResource(R.string.update_failed),
            confirm = stringResource(R.string.update_retry) to viewModel::retry,
            onDismiss = viewModel::dismiss
        )
    }
}

@Composable
private fun UpdateAlert(
    manifest: UpdateManifest,
    message: String,
    onDismiss: () -> Unit,
    progress: Float? = null,
    confirm: Pair<String, () -> Unit>? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
        title = { Text(stringResource(R.string.update_title, manifest.versionName, manifest.versionCode)) },
        text = {
            Column {
                Text(message)
                if (manifest.notes.isNotBlank()) {
                    Text(manifest.notes, modifier = Modifier.padding(top = 8.dp))
                }
                progress?.let {
                    val modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    if (it < 0f) LinearProgressIndicator(modifier) else LinearProgressIndicator({ it }, modifier)
                }
            }
        },
        confirmButton = { confirm?.let { (label, action) -> Button(onClick = action) { Text(label) } } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } }
    )
}

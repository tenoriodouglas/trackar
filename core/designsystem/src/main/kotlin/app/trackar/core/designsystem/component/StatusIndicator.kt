package app.trackar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.trackar.core.designsystem.R
import app.trackar.core.designsystem.theme.StatusColors
import app.trackar.core.designsystem.theme.TrackarTheme
import app.trackar.core.model.DeviceStatus

val DeviceStatus.color: Color
    get() = when (this) {
        DeviceStatus.ONLINE -> StatusColors.Online
        DeviceStatus.OFFLINE -> StatusColors.Offline
        DeviceStatus.UNKNOWN -> StatusColors.Unknown
    }

@Composable
fun DeviceStatus.label(): String = stringResource(
    when (this) {
        DeviceStatus.ONLINE -> R.string.core_designsystem_status_online
        DeviceStatus.OFFLINE -> R.string.core_designsystem_status_offline
        DeviceStatus.UNKNOWN -> R.string.core_designsystem_status_unknown
    }
)

@Composable
fun StatusIndicator(status: DeviceStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(Modifier.size(10.dp).background(status.color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(status.label(), style = MaterialTheme.typography.labelMedium)
    }
}

@Preview
@Composable
private fun StatusIndicatorPreview() {
    TrackarTheme { StatusIndicator(DeviceStatus.ONLINE) }
}

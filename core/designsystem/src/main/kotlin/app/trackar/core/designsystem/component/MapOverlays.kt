package app.trackar.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trackar.core.designsystem.theme.TrackarTheme
import kotlin.math.roundToInt

/**
 * Vehicle pin: a colored disc with an arrow pointing to [course] (degrees, clockwise from north).
 * Rotation is drawn inside the icon so it stays correct for north-up maps.
 */
@Composable
fun VehicleMarkerIcon(color: Color, course: Float?, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .background(color, CircleShape)
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Navigation,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f).rotate(course ?: 0f)
        )
    }
}

/** Vehicle pin with a name tag above it. */
@Composable
fun LabeledVehicleMarker(name: String, color: Color, course: Float?, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            shadowElevation = 4.dp
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        VehicleMarkerIcon(
            color = color,
            course = course,
            size = if (selected) 52.dp else 44.dp,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
    }
}

/** Round speedometer bubble, the main glanceable number on the map. */
@Composable
fun SpeedBubble(speedKmh: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(68.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = speedKmh.roundToInt().toString(),
                fontSize = 24.sp,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text("km/h", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Large round floating button used over maps. */
@Composable
fun MapRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(56.dp).shadow(6.dp, CircleShape),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Preview
@Composable
private fun MapOverlaysPreview() {
    TrackarTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LabeledVehicleMarker("Hilux", Color(0xFF2ECC71), course = 45f, selected = true)
            SpeedBubble(62.0)
        }
    }
}

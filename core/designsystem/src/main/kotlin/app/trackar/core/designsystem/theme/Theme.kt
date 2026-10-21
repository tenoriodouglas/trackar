package app.trackar.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Sky50,
    onPrimary = White,
    primaryContainer = Sky90,
    onPrimaryContainer = Ink10,
    secondary = Ink30,
    onSecondary = White,
    tertiary = Sun,
    onTertiary = Ink10,
    error = Coral,
    background = Cloud,
    onBackground = Ink10,
    surface = White,
    onSurface = Ink10,
    surfaceContainer = Cloud
)

private val DarkColors = darkColorScheme(
    primary = Sky80,
    onPrimary = Ink10,
    primaryContainer = Sky40,
    onPrimaryContainer = White,
    secondary = Sky90,
    tertiary = Sun,
    onTertiary = Ink10,
    error = Coral,
    background = Ink10,
    surface = Ink20,
    surfaceContainer = Ink20
)

// Generous rounding gives the playful, finger-friendly navigation look.
private val TrackarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val TrackarTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
fun TrackarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors

        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = TrackarTypography, shapes = TrackarShapes, content = content)
}

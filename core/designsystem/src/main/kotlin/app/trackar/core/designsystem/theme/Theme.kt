package app.trackar.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Navy40,
    onPrimary = Neutral99,
    primaryContainer = Navy90,
    onPrimaryContainer = Navy10,
    secondary = Green40,
    secondaryContainer = Green90,
    error = Red40,
    background = Neutral99,
    onBackground = Neutral10
)

private val DarkColors = darkColorScheme(
    primary = Navy80,
    onPrimary = Navy20,
    primaryContainer = Navy20,
    onPrimaryContainer = Navy90,
    secondary = Green80,
    error = Red80
)

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
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}

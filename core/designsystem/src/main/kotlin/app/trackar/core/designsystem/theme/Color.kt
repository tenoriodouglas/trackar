package app.trackar.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Brand palette: deep navy + signal green.
internal val Navy10 = Color(0xFF001A33)
internal val Navy20 = Color(0xFF00305C)
internal val Navy40 = Color(0xFF1B5FA8)
internal val Navy80 = Color(0xFFA6C8FF)
internal val Navy90 = Color(0xFFD5E3FF)
internal val Green40 = Color(0xFF1F8A4C)
internal val Green80 = Color(0xFF8CD9A6)
internal val Green90 = Color(0xFFA8F5C1)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
internal val Neutral10 = Color(0xFF1A1C1E)
internal val Neutral99 = Color(0xFFFCFCFF)

/** Semantic colors for vehicle status, independent of the Material scheme. */
object StatusColors {
    val Online = Color(0xFF2E9E5B)
    val Offline = Color(0xFF8A8F98)
    val Unknown = Color(0xFFE0A100)
}

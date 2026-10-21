package app.trackar.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Bright, friendly navigation palette: sky blue + deep ink, with playful accents.
internal val Sky40 = Color(0xFF0A9BD8)
internal val Sky50 = Color(0xFF12B5F0)
internal val Sky80 = Color(0xFF8FDBFF)
internal val Sky90 = Color(0xFFD6F2FF)
internal val Ink10 = Color(0xFF0E1A2B)
internal val Ink20 = Color(0xFF1B2A40)
internal val Ink30 = Color(0xFF2A3B55)
internal val Sun = Color(0xFFFFC93C)
internal val Coral = Color(0xFFFF5A5F)
internal val Cloud = Color(0xFFF4F8FB)
internal val White = Color(0xFFFFFFFF)

/** Semantic colors for vehicle status, independent of the Material scheme. */
object StatusColors {
    val Online = Color(0xFF2ECC71)
    val Offline = Color(0xFF9AA5B1)
    val Unknown = Color(0xFFFFB020)
}

/** Map overlay colors (route, markers), shared by map-based features. */
object MapColors {
    val Route = Sky50
    val RouteOutline = Ink20
    val Start = Color(0xFF2ECC71)
    val End = Coral
    val Stop = Sun
}

package app.trackar.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Map
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import app.trackar.R
import app.trackar.feature.map.navigation.MapNavKey
import app.trackar.feature.vehicles.navigation.VehicleListNavKey

enum class TopLevelDestination(
    val key: NavKey,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    MAP(MapNavKey, R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map),
    VEHICLES(VehicleListNavKey, R.string.nav_vehicles, Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar)
}

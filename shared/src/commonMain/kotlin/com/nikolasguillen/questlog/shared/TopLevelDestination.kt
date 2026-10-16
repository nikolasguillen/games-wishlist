package com.nikolasguillen.questlog.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.nikolasguillen.questlog.core.navigation.ListsRoute
import com.nikolasguillen.questlog.core.navigation.RadarRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import com.nikolasguillen.questlog.core.navigation.WishlistRoute
import com.nikolasguillen.questlog.shared.resources.Res
import com.nikolasguillen.questlog.shared.resources.lists_nav_bar_item
import com.nikolasguillen.questlog.shared.resources.radar_nav_bar_item
import com.nikolasguillen.questlog.shared.resources.search_nav_bar_item
import org.jetbrains.compose.resources.StringResource

/**
 * The app's top-level destinations, in the order they appear in the bottom bar and in the side rail, so the two
 * navigation components cannot drift apart.
 */
internal enum class TopLevelDestination(
    val route: NavKey,
    val icon: ImageVector,
    val label: StringResource
) {
    SEARCH(SearchRoute, Icons.Default.Search, Res.string.search_nav_bar_item),
    RADAR(RadarRoute, Icons.Default.Radar, Res.string.radar_nav_bar_item),
    LISTS(ListsRoute, Icons.AutoMirrored.Filled.List, Res.string.lists_nav_bar_item);

    /** Whether [currentRoute] belongs to this destination, which is not only [route] itself. */
    fun isSelected(currentRoute: NavKey): Boolean = when (this) {
        SEARCH -> currentRoute is SearchRoute
        RADAR -> currentRoute is RadarRoute
        // The wishlist is opened from Lists, so Lists stays highlighted while it is on screen.
        LISTS -> currentRoute is ListsRoute || currentRoute is WishlistRoute
    }
}

package com.nikolasguillen.questlog.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.nikolasguillen.questlog.core.designsystem.theme.AppComponentsColors
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.navigation.ListsRoute
import com.nikolasguillen.questlog.core.navigation.RadarRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import com.nikolasguillen.questlog.core.navigation.WishlistRoute
import com.nikolasguillen.questlog.shared.resources.Res
import com.nikolasguillen.questlog.shared.resources.lists_nav_bar_item
import com.nikolasguillen.questlog.shared.resources.radar_nav_bar_item
import com.nikolasguillen.questlog.shared.resources.search_nav_bar_item
import org.jetbrains.compose.resources.stringResource

@Composable
fun QuestLogBottomBar(
    backStack: NavBackStack<NavKey>,
    onNavigateToRoute: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRoute = backStack.last()
    
    NavigationBar(
        containerColor = MaterialTheme.appColors.navBarContainerColor,
        modifier = modifier
    ) {
        NavigationBarItem(
            selected = currentRoute is SearchRoute,
            onClick = {
                if (currentRoute !is SearchRoute) {
                    onNavigateToRoute(SearchRoute)
                }
            },
            icon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(Res.string.search_nav_bar_item)
                )
            },
            label = { Text(stringResource(Res.string.search_nav_bar_item)) },
            colors = AppComponentsColors.navBarItemColors
        )
        NavigationBarItem(
            selected = currentRoute is RadarRoute,
            onClick = {
                if (currentRoute !is RadarRoute) {
                    onNavigateToRoute(RadarRoute)
                }
            },
            icon = {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = stringResource(Res.string.radar_nav_bar_item)
                )
            },
            label = { Text(stringResource(Res.string.radar_nav_bar_item)) },
            colors = AppComponentsColors.navBarItemColors
        )
        NavigationBarItem(
            selected = currentRoute is ListsRoute || currentRoute is WishlistRoute,
            onClick = {
                if (currentRoute !is ListsRoute) {
                    onNavigateToRoute(ListsRoute)
                }
            },
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(Res.string.lists_nav_bar_item)
                )
            },
            label = { Text(stringResource(Res.string.lists_nav_bar_item)) },
            colors = AppComponentsColors.navBarItemColors
        )
    }
}

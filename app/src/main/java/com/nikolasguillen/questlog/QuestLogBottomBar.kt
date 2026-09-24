package com.nikolasguillen.questlog

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
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.nikolasguillen.questlog.core.designsystem.theme.AppComponentsColors
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.navigation.ListsRoute
import com.nikolasguillen.questlog.core.navigation.RadarRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import com.nikolasguillen.questlog.core.navigation.WishlistRoute

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
                    contentDescription = stringResource(R.string.search_nav_bar_item)
                )
            },
            label = { Text(stringResource(R.string.search_nav_bar_item)) },
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
                    contentDescription = stringResource(R.string.radar_nav_bar_item)
                )
            },
            label = { Text(stringResource(R.string.radar_nav_bar_item)) },
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
                    contentDescription = stringResource(R.string.lists_nav_bar_item)
                )
            },
            label = { Text(stringResource(R.string.lists_nav_bar_item)) },
            colors = AppComponentsColors.navBarItemColors
        )
    }
}

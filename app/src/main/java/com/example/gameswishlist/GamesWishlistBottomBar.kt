package com.example.gameswishlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import com.example.gameswishlist.core.designsystem.theme.AppComponentsColors
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.navigation.ListsRoute
import com.example.gameswishlist.core.navigation.SearchRoute
import com.example.gameswishlist.core.navigation.WishlistRoute

@Composable
fun GamesWishlistBottomBar(
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

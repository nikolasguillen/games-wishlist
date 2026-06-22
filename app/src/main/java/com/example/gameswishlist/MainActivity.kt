package com.example.gameswishlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.gameswishlist.core.designsystem.theme.AppComponentsColors
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.navigation.GameDetailRoute
import com.example.gameswishlist.core.navigation.ListsRoute
import com.example.gameswishlist.core.navigation.SearchRoute
import com.example.gameswishlist.core.navigation.WishlistRoute
import com.example.gameswishlist.feature.gamedetail.GameDetailScreen
import com.example.gameswishlist.feature.gamedetail.GameDetailViewModel
import com.example.gameswishlist.feature.lists.ListsScreen
import com.example.gameswishlist.feature.lists.ListsViewModel
import com.example.gameswishlist.feature.search.SearchScreen
import com.example.gameswishlist.feature.search.SearchViewModel
import com.example.gameswishlist.feature.wishlist.WishlistScreen
import com.example.gameswishlist.feature.wishlist.WishlistViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            GamesWishlistTheme {
                MainContent()

                StatusBarProtection()
            }
        }
    }
}

@Composable
fun MainContent() {
    val backStack = rememberNavBackStack(SearchRoute as NavKey)
    Scaffold(
        containerColor = MaterialTheme.appColors.appBackground,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.appColors.navBarContainerColor
            ) {
                NavigationBarItem(
                    selected = backStack.last() is SearchRoute,
                    onClick = {
                        if (backStack.last() !is SearchRoute) {
                            backStack.add(SearchRoute)
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
                    selected = backStack.last() is ListsRoute || backStack.last() is WishlistRoute,
                    onClick = {
                        if (backStack.last() !is ListsRoute) {
                            backStack.add(ListsRoute)
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
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.size - 1)
                }
            },
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .consumeWindowInsets(innerPadding),
            entryProvider = { key ->
                when (key) {
                    is SearchRoute -> NavEntry(key) {
                        val vm: SearchViewModel = hiltViewModel()
                        SearchScreen(
                            viewModel = vm,
                            onGameClick = { gameId: Int ->
                                backStack.add(GameDetailRoute(gameId))
                            }
                        )
                    }

                    is ListsRoute -> NavEntry(key) {
                        val vm: ListsViewModel = hiltViewModel()
                        ListsScreen(
                            viewModel = vm,
                            onListClick = { listId: Long, listName: String ->
                                backStack.add(WishlistRoute(listId, listName))
                            }
                        )
                    }

                    is WishlistRoute -> NavEntry(key) {
                        val vm: WishlistViewModel = hiltViewModel()
                        WishlistScreen(
                            listId = key.listId,
                            listName = key.listName,
                            viewModel = vm,
                            onGameClick = { gameId: Int ->
                                backStack.add(GameDetailRoute(gameId))
                            },
                            onBackClick = {
                                backStack.removeAt(backStack.size - 1)
                            }
                        )
                    }

                    is GameDetailRoute -> NavEntry(key) {
                        val vm: GameDetailViewModel = hiltViewModel()
                        GameDetailScreen(
                            gameId = key.gameId,
                            viewModel = vm,
                            onBackClick = {
                                if (backStack.size > 1) {
                                    backStack.removeAt(backStack.size - 1)
                                }
                            }
                        )
                    }

                    else -> NavEntry(key) { }
                }
            }
        )
    }
}

@Composable
private fun StatusBarProtection(
    color: Color = MaterialTheme.appColors.appBackground,
) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                with(LocalDensity.current) {
                    (WindowInsets.statusBars.getTop(this) * 1.2f).toDp()
                }
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 1f),
                        color.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                )
            )
    )
}
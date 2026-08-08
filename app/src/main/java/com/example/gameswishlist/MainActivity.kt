package com.example.gameswishlist

import android.os.Build
import android.os.Bundle
import android.view.RoundedCorner
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val backStack = rememberNavBackStack(SearchRoute as NavKey)
    Scaffold(
        containerColor = MaterialTheme.appColors.appBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = backStack.last() is SearchRoute || backStack.last() is ListsRoute,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                GamesWishlistBottomBar(
                    backStack = backStack,
                    onNavigateToRoute = { route ->
                        if (backStack.lastOrNull() != route) {
                            // Pop everything back to the root (SearchRoute)
                            while (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            // If the new route is not the root, add it
                            if (route != SearchRoute) {
                                backStack.add(route)
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val view = LocalView.current
        val density = LocalDensity.current
        val cornerRadius = remember(view, density) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val insets = view.rootWindowInsets
                val radiusPx =
                    insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
                with(density) { radiusPx.toDp() }
            } else {
                0.dp
            }
        }

        val cornerClipModifier = Modifier.graphicsLayer {
            shape = RoundedCornerShape(cornerRadius)
            clip = true
        }

        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            predictivePopTransitionSpec = {
                (slideInHorizontally { -it } + fadeIn(
                    tween(
                        durationMillis = 400,
                        delayMillis = 200
                    )
                )) togetherWith
                        (slideOutHorizontally { it } + fadeOut(tween(durationMillis = 200)))
            },
            entryProvider = { key ->
                when (key) {
                    is SearchRoute -> NavEntry(key) {
                        val vm = hiltViewModel<SearchViewModel>()
                        SearchScreen(
                            viewModel = vm,
                            onGameClick = { gameId: Int ->
                                val nextRoute = GameDetailRoute(gameId)
                                if (backStack.lastOrNull() != nextRoute) {
                                    backStack.add(nextRoute)
                                }
                            },
                            modifier = cornerClipModifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        )
                    }

                    is ListsRoute -> NavEntry(key) {
                        val vm = hiltViewModel<ListsViewModel>()
                        ListsScreen(
                            viewModel = vm,
                            onListClick = { listId: Long ->
                                val nextRoute = WishlistRoute(listId)
                                if (backStack.lastOrNull() != nextRoute) {
                                    backStack.add(nextRoute)
                                }
                            },
                            modifier = Modifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        )
                    }

                    is WishlistRoute -> NavEntry(key) {
                        val vm = hiltViewModel<WishlistViewModel, WishlistViewModel.Factory>(
                            creationCallback = { factory ->
                                factory.create(key.listId)
                            }
                        )

                        WishlistScreen(
                            viewModel = vm,
                            onGameClick = { gameId: Int ->
                                val nextRoute = GameDetailRoute(gameId)
                                if (backStack.lastOrNull() != nextRoute) {
                                    backStack.add(nextRoute)
                                }
                            },
                            onBackClick = { backStack.removeLastOrNull() }
                        )
                    }

                    is GameDetailRoute -> NavEntry(key) {
                        val vm = hiltViewModel<GameDetailViewModel, GameDetailViewModel.Factory>(
                            creationCallback = { factory ->
                                factory.create(key.gameId)
                            }
                        )
                        GameDetailScreen(
                            viewModel = vm,
                            onBackClick = { backStack.removeLastOrNull() },
                            onGameClick = { gameId: Int ->
                                val nextRoute = GameDetailRoute(gameId)
                                if (backStack.lastOrNull() != nextRoute) {
                                    backStack.add(nextRoute)
                                }
                            },
                            modifier = cornerClipModifier
                        )
                    }

                    else -> NavEntry(key) { }
                }
            }
        )
    }
}

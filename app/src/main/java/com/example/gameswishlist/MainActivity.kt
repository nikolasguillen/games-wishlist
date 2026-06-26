package com.example.gameswishlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
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
                        backStack.add(route)
                    }
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
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            popTransitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            predictivePopTransitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            entryProvider = { key ->
                when (key) {
                    is SearchRoute -> NavEntry(key) {
                        val vm: SearchViewModel = hiltViewModel()
                        SearchScreen(
                            viewModel = vm,
                            onGameClick = { gameId: Int ->
                                backStack.add(GameDetailRoute(gameId))
                            },
                            modifier = Modifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        )
                    }

                    is ListsRoute -> NavEntry(key) {
                        val vm: ListsViewModel = hiltViewModel()
                        ListsScreen(
                            viewModel = vm,
                            onListClick = { listId: Long, listName: String ->
                                backStack.add(WishlistRoute(listId, listName))
                            },
                            modifier = Modifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
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

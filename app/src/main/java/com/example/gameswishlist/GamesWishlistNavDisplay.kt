package com.example.gameswishlist

import android.annotation.SuppressLint
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.gameswishlist.core.navigation.GameDetailRoute
import com.example.gameswishlist.core.navigation.ListsRoute
import com.example.gameswishlist.core.navigation.OwnedPlatformsRoute
import com.example.gameswishlist.core.navigation.SearchRoute
import com.example.gameswishlist.core.navigation.SettingsRoute
import com.example.gameswishlist.core.navigation.WishlistRoute
import com.example.gameswishlist.feature.gamedetail.GameDetailScreen
import com.example.gameswishlist.feature.gamedetail.GameDetailViewModel
import com.example.gameswishlist.feature.lists.ListsScreen
import com.example.gameswishlist.feature.lists.ListsViewModel
import com.example.gameswishlist.feature.search.SearchScreen
import com.example.gameswishlist.feature.search.SearchViewModel
import com.example.gameswishlist.feature.settings.OwnedPlatformsScreen
import com.example.gameswishlist.feature.settings.OwnedPlatformsViewModel
import com.example.gameswishlist.feature.settings.SettingsScreen
import com.example.gameswishlist.feature.settings.SettingsViewModel
import com.example.gameswishlist.feature.wishlist.WishlistScreen
import com.example.gameswishlist.feature.wishlist.WishlistViewModel

@Composable
fun GamesWishlistNavDisplay(
    backStack: NavBackStack<NavKey>,
    innerPadding: PaddingValues,
    @SuppressLint("ModifierParameter") cornerClipModifier: Modifier,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        modifier = modifier,
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
                        onProfileClick = {
                            if (backStack.lastOrNull() != SettingsRoute) {
                                backStack.add(SettingsRoute)
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
                        onProfileClick = {
                            if (backStack.lastOrNull() != SettingsRoute) {
                                backStack.add(SettingsRoute)
                            }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }

                is SettingsRoute -> NavEntry(key) {
                    val vm = hiltViewModel<SettingsViewModel>()
                    SettingsScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() },
                        onOwnedPlatformsClick = {
                            if (backStack.lastOrNull() != OwnedPlatformsRoute) {
                                backStack.add(OwnedPlatformsRoute)
                            }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }

                is OwnedPlatformsRoute -> NavEntry(key) {
                    val vm = hiltViewModel<OwnedPlatformsViewModel>()
                    OwnedPlatformsScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() },
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

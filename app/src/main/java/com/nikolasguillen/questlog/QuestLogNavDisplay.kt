package com.nikolasguillen.questlog

import android.annotation.SuppressLint
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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nikolasguillen.questlog.core.navigation.GameDetailRoute
import com.nikolasguillen.questlog.core.navigation.ListsRoute
import com.nikolasguillen.questlog.core.navigation.OnboardingRoute
import com.nikolasguillen.questlog.core.navigation.OwnedPlatformsRoute
import com.nikolasguillen.questlog.core.navigation.RadarRoute
import com.nikolasguillen.questlog.core.navigation.ReleaseNotificationsRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import com.nikolasguillen.questlog.core.navigation.SettingsRoute
import com.nikolasguillen.questlog.core.navigation.WishlistRoute
import com.nikolasguillen.questlog.feature.gamedetail.GameDetailScreen
import com.nikolasguillen.questlog.feature.gamedetail.GameDetailViewModel
import com.nikolasguillen.questlog.feature.lists.ListsScreen
import com.nikolasguillen.questlog.feature.lists.ListsViewModel
import com.nikolasguillen.questlog.feature.onboarding.OnboardingScreen
import com.nikolasguillen.questlog.feature.onboarding.OnboardingViewModel
import com.nikolasguillen.questlog.feature.radar.RadarScreen
import com.nikolasguillen.questlog.feature.radar.RadarViewModel
import com.nikolasguillen.questlog.feature.search.SearchScreen
import com.nikolasguillen.questlog.feature.search.SearchViewModel
import com.nikolasguillen.questlog.feature.settings.OwnedPlatformsScreen
import com.nikolasguillen.questlog.feature.settings.OwnedPlatformsViewModel
import com.nikolasguillen.questlog.feature.settings.ReleaseNotificationsScreen
import com.nikolasguillen.questlog.feature.settings.ReleaseNotificationsViewModel
import com.nikolasguillen.questlog.feature.settings.SettingsScreen
import com.nikolasguillen.questlog.feature.settings.SettingsViewModel
import com.nikolasguillen.questlog.feature.wishlist.WishlistScreen
import com.nikolasguillen.questlog.feature.wishlist.WishlistViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Suppress("ParamsComparedByRef")
@Composable
fun QuestLogNavDisplay(
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
        predictivePopTransitionSpec = { (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut()) },
        entryProvider = { key ->
            when (key) {
                is SearchRoute -> NavEntry(key) {
                    val vm = koinViewModel<SearchViewModel>()
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

                is RadarRoute -> NavEntry(key) {
                    val vm = koinViewModel<RadarViewModel>()
                    RadarScreen(
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
                    val vm = koinViewModel<ListsViewModel>()
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
                    val vm = koinViewModel<SettingsViewModel>()
                    SettingsScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() },
                        onOwnedPlatformsClick = {
                            if (backStack.lastOrNull() != OwnedPlatformsRoute) {
                                backStack.add(OwnedPlatformsRoute)
                            }
                        },
                        onReleaseNotificationsClick = {
                            if (backStack.lastOrNull() != ReleaseNotificationsRoute) {
                                backStack.add(ReleaseNotificationsRoute)
                            }
                        },
                        onShowWelcomeTourClick = {
                            if (backStack.lastOrNull() != OnboardingRoute) {
                                backStack.add(OnboardingRoute)
                            }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }

                is OwnedPlatformsRoute -> NavEntry(key) {
                    val vm = koinViewModel<OwnedPlatformsViewModel>()
                    OwnedPlatformsScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() },
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }

                is ReleaseNotificationsRoute -> NavEntry(key) {
                    val vm = koinViewModel<ReleaseNotificationsViewModel>()
                    ReleaseNotificationsScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() },
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }

                is WishlistRoute -> NavEntry(key) {
                    val vm = koinViewModel<WishlistViewModel> { parametersOf(key.listId) }

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
                    val vm = koinViewModel<GameDetailViewModel> { parametersOf(key.gameId) }
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

                is OnboardingRoute -> NavEntry(key) {
                    val vm = koinViewModel<OnboardingViewModel>()
                    OnboardingScreen(
                        viewModel = vm,
                        onFinish = {
                            // First launch: the flow is the only entry, so Search takes its place and back
                            // can never return into it. Replayed from Settings there is something underneath
                            // to go back to.
                            if (backStack.size == 1) {
                                backStack.add(SearchRoute)
                                backStack.removeAt(0)
                            } else {
                                backStack.removeLastOrNull()
                            }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }

                else -> NavEntry(key) { }
            }
        }
    )
}

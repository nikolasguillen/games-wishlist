package com.nikolasguillen.questlog.shared

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.nikolasguillen.questlog.core.navigation.AccountBackupRoute
import com.nikolasguillen.questlog.core.navigation.CharacterDetailRoute
import com.nikolasguillen.questlog.core.navigation.DeveloperDetailRoute
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
import com.nikolasguillen.questlog.feature.settings.AccountBackupScreen
import com.nikolasguillen.questlog.feature.settings.AccountBackupViewModel
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

/**
 * The single place that turns a back stack into screens.
 *
 * @param twoPaneEnabled Whether the window is wide enough to show a game opened from Search beside the results.
 * Below that width every game opens full-screen and back pops one entry, as it always did.
 */
@Suppress("ParamsComparedByRef")
@Composable
fun QuestLogNavDisplay(
    backStack: NavBackStack<NavKey>,
    twoPaneEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val sceneStrategy = remember(twoPaneEnabled) { SearchDetailSceneStrategy(twoPaneEnabled) }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { navigateBack(backStack, twoPaneEnabled) },
        sceneStrategies = listOf(sceneStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        popTransitionSpec = { fadeIn() togetherWith fadeOut() },
        predictivePopTransitionSpec = { (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut()) },
        entryProvider = { key ->
            when (key) {
                is SearchRoute -> NavEntry(key, metadata = searchPaneMetadata()) {
                    val vm = koinViewModel<SearchViewModel>()
                    SearchScreen(
                        viewModel = vm,
                        onGameClick = { gameId: Int ->
                            openSearchDetail(backStack, gameId, twoPaneEnabled)
                        },
                        onCharacterClick = { characterId: Int ->
                            val nextRoute = CharacterDetailRoute(characterId)
                            if (backStack.lastOrNull() != nextRoute) {
                                backStack.add(nextRoute)
                            }
                        },
                        onDeveloperClick = { developerId: Int ->
                            val nextRoute = DeveloperDetailRoute(developerId)
                            if (backStack.lastOrNull() != nextRoute) {
                                backStack.add(nextRoute)
                            }
                        },
                        onProfileClick = {
                            if (backStack.lastOrNull() != SettingsRoute) {
                                backStack.add(SettingsRoute)
                            }
                        }
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
                        }
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
                        }
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
                        onAccountBackupClick = {
                            if (backStack.lastOrNull() != AccountBackupRoute) {
                                backStack.add(AccountBackupRoute)
                            }
                        },
                        onShowWelcomeTourClick = {
                            if (backStack.lastOrNull() != OnboardingRoute) {
                                backStack.add(OnboardingRoute)
                            }
                        }
                    )
                }

                is OwnedPlatformsRoute -> NavEntry(key) {
                    val vm = koinViewModel<OwnedPlatformsViewModel>()
                    OwnedPlatformsScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() }
                    )
                }

                is ReleaseNotificationsRoute -> NavEntry(key) {
                    val vm = koinViewModel<ReleaseNotificationsViewModel>()
                    ReleaseNotificationsScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() }
                    )
                }

                is AccountBackupRoute -> NavEntry(key) {
                    val vm = koinViewModel<AccountBackupViewModel>()
                    AccountBackupScreen(
                        viewModel = vm,
                        onBackClick = { backStack.removeLastOrNull() }
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

                is GameDetailRoute -> NavEntry(
                    key,
                    metadata = if (key.fromSearch) searchDetailMetadata() else emptyMap()
                ) {
                    val vm = koinViewModel<GameDetailViewModel> { parametersOf(key.gameId) }
                    // A game opened from Search on a wide window is drawn beside the results, which are already on
                    // screen, so there is nothing to go back to: it gets a close button for the pane, which does what
                    // system back does, and no back button.
                    val inPane = twoPaneEnabled && key.fromSearch
                    GameDetailScreen(
                        viewModel = vm,
                        onBackClick = if (inPane) null else {
                            { navigateBack(backStack, twoPaneEnabled) }
                        },
                        onCloseClick = if (inPane) {
                            { navigateBack(backStack, twoPaneEnabled) }
                        } else {
                            null
                        },
                        onGameClick = { gameId: Int ->
                            if (key.fromSearch) {
                                // Replaces the right pane on a wide window and stacks below it.
                                openSearchDetail(backStack, gameId, twoPaneEnabled)
                            } else {
                                val nextRoute = GameDetailRoute(gameId)
                                if (backStack.lastOrNull() != nextRoute) {
                                    backStack.add(nextRoute)
                                }
                            }
                        }
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
                        }
                    )
                }

                else -> NavEntry(key) { }
            }
        }
    )
}

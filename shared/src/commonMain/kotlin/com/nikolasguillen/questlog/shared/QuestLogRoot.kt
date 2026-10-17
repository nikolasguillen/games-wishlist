package com.nikolasguillen.questlog.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.model.AppearanceMode
import com.nikolasguillen.questlog.core.navigation.GameDetailRoute
import com.nikolasguillen.questlog.core.navigation.GameNavSavedStateConfiguration
import com.nikolasguillen.questlog.core.navigation.ListsRoute
import com.nikolasguillen.questlog.core.navigation.OnboardingRoute
import com.nikolasguillen.questlog.core.navigation.RadarRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import org.koin.compose.viewmodel.koinViewModel

/**
 * The whole app, for every platform: the theme, the navigation suite (a bottom bar or a side rail, by window size),
 * and the navigation display.
 *
 * The light/dark decision is made here, once, from the user's [AppearanceMode]; everything below reads
 * `MaterialTheme.isDarkTheme`. Nothing is drawn until the onboarding flag is known, because it picks the back
 * stack's first entry.
 *
 * @param pendingDeepLinkGameId A game a release reminder asked to open, handed in as state because the platform
 * delivers it outside composition. It is dropped, not queued, while the welcome flow is still the first entry.
 */
@Composable
fun QuestLogRoot(
    pendingDeepLinkGameId: Int?,
    onDeepLinkConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    rootViewModel: RootViewModel = koinViewModel()
) {
    val uiState by rootViewModel.uiState.collectAsStateWithLifecycle()
    val darkTheme = when (uiState.appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }

    QuestLogTheme(darkTheme = darkTheme) {
        uiState.onboardingCompleted?.let { completed ->
            QuestLogContent(
                pendingDeepLinkGameId = pendingDeepLinkGameId,
                onDeepLinkConsumed = onDeepLinkConsumed,
                startWithOnboarding = !completed,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun QuestLogContent(
    pendingDeepLinkGameId: Int?,
    onDeepLinkConsumed: () -> Unit,
    startWithOnboarding: Boolean,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(
        GameNavSavedStateConfiguration,
        (if (startWithOnboarding) OnboardingRoute else SearchRoute) as NavKey
    )

    LaunchedEffect(pendingDeepLinkGameId) {
        val gameId = pendingDeepLinkGameId ?: return@LaunchedEffect
        // The reminder this link belongs to cannot exist before the flow was ever completed, so the link is
        // dropped rather than queued behind it.
        if (backStack.firstOrNull() is OnboardingRoute) {
            onDeepLinkConsumed()
            return@LaunchedEffect
        }
        val nextRoute = GameDetailRoute(gameId)
        if (backStack.lastOrNull() != nextRoute) {
            backStack.add(nextRoute)
        }
        onDeepLinkConsumed()
    }

    val currentRoute = backStack.last()
    val isTopLevelRoute = currentRoute is SearchRoute ||
            currentRoute is RadarRoute ||
            currentRoute is ListsRoute

    val onNavigateToRoute: (NavKey) -> Unit = { route ->
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

    QuestLogNavigationSuite(
        currentRoute = currentRoute,
        isVisible = isTopLevelRoute,
        onNavigateToRoute = onNavigateToRoute,
        modifier = modifier
    ) {
        QuestLogNavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize()
        )
    }
}

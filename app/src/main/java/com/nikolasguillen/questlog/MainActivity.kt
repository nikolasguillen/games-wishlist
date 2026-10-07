package com.nikolasguillen.questlog

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.RoundedCorner
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.domain.usecase.settings.GetAppearanceModeUseCase
import com.nikolasguillen.questlog.core.domain.usecase.settings.GetOnboardingCompletedUseCase
import com.nikolasguillen.questlog.core.model.AppearanceMode
import com.nikolasguillen.questlog.core.navigation.GameDetailRoute
import com.nikolasguillen.questlog.core.navigation.ListsRoute
import com.nikolasguillen.questlog.core.navigation.OnboardingRoute
import com.nikolasguillen.questlog.core.navigation.RadarRoute
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

// Long enough for the wordmark to register on a fresh launch, short enough not to feel like a wait.
private const val MIN_SPLASH_DURATION_MS = 500L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var getAppearanceModeUseCase: GetAppearanceModeUseCase

    @Inject
    lateinit var getOnboardingCompletedUseCase: GetOnboardingCompletedUseCase

    // `null` until the stored flag has been read. The back stack's first entry is chosen from it, and
    // rememberNavBackStack only looks at its initial key once, so nothing may compose before it is known.
    private var onboardingCompleted by mutableStateOf<Boolean?>(null)

    // A release notification's PendingIntent carries a questlog://game/<id> deep link; read here (outside
    // Compose) and handed to MainContent as state, since onNewIntent -- the warm-start case -- never runs
    // inside composition.
    private var pendingDeepLinkGameId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keeps the splash on screen for the few milliseconds the flag read takes, instead of drawing one
        // empty frame (which would end the splash early and flicker) or blocking the main thread.
        installSplashScreen().setKeepOnScreenCondition { onboardingCompleted == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        pendingDeepLinkGameId = intent.toGameDeepLinkId()
        lifecycleScope.launch {
            // The minimum runs alongside the read, so it only delays a launch the read would have finished
            // sooner. It is skipped on recreation (rotation, night-mode change): no splash covers that one,
            // and holding the first frame would blank the screen.
            val completed = async { getOnboardingCompletedUseCase().first() }
            if (savedInstanceState == null) delay(MIN_SPLASH_DURATION_MS.milliseconds)
            onboardingCompleted = completed.await()
        }
        setContent {
            val appearanceMode by getAppearanceModeUseCase()
                .collectAsStateWithLifecycle(initialValue = AppearanceMode.SYSTEM)
            val darkTheme = when (appearanceMode) {
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            }
            QuestLogTheme(darkTheme = darkTheme) {
                onboardingCompleted?.let { completed ->
                    MainContent(
                        pendingDeepLinkGameId = pendingDeepLinkGameId,
                        onDeepLinkConsumed = { pendingDeepLinkGameId = null },
                        startWithOnboarding = !completed
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkGameId = intent.toGameDeepLinkId()
    }
}

private fun Intent.toGameDeepLinkId(): Int? {
    val uri = data ?: return null
    if (uri.scheme != "questlog" || uri.host != "game") return null
    return uri.lastPathSegment?.toIntOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    pendingDeepLinkGameId: Int? = null,
    onDeepLinkConsumed: () -> Unit = {},
    startWithOnboarding: Boolean = false
) {
    val backStack = rememberNavBackStack(
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = backStack.last() is SearchRoute ||
                        backStack.last() is RadarRoute ||
                        backStack.last() is ListsRoute,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                QuestLogBottomBar(
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

        QuestLogNavDisplay(
            backStack = backStack,
            innerPadding = innerPadding,
            cornerClipModifier = cornerClipModifier,
            modifier = Modifier.fillMaxSize()
        )
    }
}

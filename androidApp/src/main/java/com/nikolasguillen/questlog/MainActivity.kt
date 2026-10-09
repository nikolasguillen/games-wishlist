package com.nikolasguillen.questlog

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.RoundedCorner
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.nikolasguillen.questlog.shared.QuestLogRoot
import com.nikolasguillen.questlog.shared.RootViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.time.Duration.Companion.milliseconds

// Long enough for the wordmark to register on a fresh launch, short enough not to feel like a wait.
private const val MIN_SPLASH_DURATION_MS = 500L

class MainActivity : ComponentActivity() {

    // The same instance QuestLogRoot reads: both resolve it against this activity.
    private val rootViewModel: RootViewModel by viewModel()

    // True once the stored onboarding flag has been read and, on a fresh launch, the splash has been up for its
    // minimum time. The back stack's first entry is chosen from that flag, so nothing may compose before it.
    private var splashDone = false

    // A release notification's PendingIntent carries a questlog://game/<id> deep link; read here (outside
    // Compose) and handed to QuestLogRoot as state, since onNewIntent -- the warm-start case -- never runs
    // inside composition.
    private var pendingDeepLinkGameId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keeps the splash on screen for the few milliseconds the flag read takes, instead of drawing one
        // empty frame (which would end the splash early and flicker) or blocking the main thread.
        installSplashScreen().setKeepOnScreenCondition { !splashDone }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        pendingDeepLinkGameId = intent.toGameDeepLinkId()
        lifecycleScope.launch {
            // The minimum runs alongside the read, so it only delays a launch the read would have finished
            // sooner. It is skipped on recreation (rotation, night-mode change): no splash covers that one,
            // and holding the first frame would blank the screen.
            val read = async { rootViewModel.uiState.first { it.onboardingCompleted != null } }
            if (savedInstanceState == null) delay(MIN_SPLASH_DURATION_MS.milliseconds)
            read.await()
            splashDone = true
        }
        setContent {
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
            QuestLogRoot(
                pendingDeepLinkGameId = pendingDeepLinkGameId,
                onDeepLinkConsumed = { pendingDeepLinkGameId = null },
                displayCornerRadius = cornerRadius
            )
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

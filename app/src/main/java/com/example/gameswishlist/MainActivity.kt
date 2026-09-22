package com.example.gameswishlist

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.navigation.ListsRoute
import com.example.gameswishlist.core.navigation.RadarRoute
import com.example.gameswishlist.core.navigation.SearchRoute
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = backStack.last() is SearchRoute ||
                        backStack.last() is RadarRoute ||
                        backStack.last() is ListsRoute,
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

        GamesWishlistNavDisplay(
            backStack = backStack,
            innerPadding = innerPadding,
            cornerClipModifier = cornerClipModifier,
            modifier = Modifier.fillMaxSize()
        )
    }
}

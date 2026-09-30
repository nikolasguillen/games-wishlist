package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.ui.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * A reusable layout for detail screens featuring an immersive hero header and a sliding sheet content.
 *
 * @param title The title to display in the TopAppBar when it becomes opaque.
 * @param onBackClick Callback for the back navigation button.
 * @param modifier The modifier to be applied to the layout.
 * @param headerHeight The height of the hero header area.
 * @param hazeState When provided, registers the scrollable content as a source so callers can blur it from behind floating elements (e.g. via `hazeBlur`).
 * @param heroContent Composable for the background/hero area. It receives a provider for the current scroll offset.
 * @param actions Composable for the TopAppBar actions. It receives the current TopAppBar alpha (0.0 to 1.0).
 * @param content Composable for the main scrollable content (the "sheet").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveDetailLayout(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    headerHeight: Dp = 350.dp,
    hazeState: HazeState? = null,
    heroContent: @Composable (scrollOffsetProvider: () -> Int) -> Unit,
    actions: @Composable RowScope.(alpha: Float) -> Unit = {},
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val headerHeightPx = with(density) { headerHeight.toPx() }
    val titleThresholdPx = headerHeightPx - with(density) { 100.dp.toPx() }
    val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer

    // Calculate TopBar alpha based on scroll position
    val topBarAlpha by remember {
        derivedStateOf {
            val progress = (scrollState.value - titleThresholdPx) / 100f
            progress.coerceIn(0f, 1f)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = topBarAlpha }
                    )
                },
                navigationIcon = {
                    ImmersiveDetailLayoutDefaults.ActionIconButton(
                        onClick = onBackClick,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_content_description),
                        alpha = topBarAlpha
                    )
                },
                actions = { actions(topBarAlpha) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarContainerColor.copy(alpha = topBarAlpha),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha),
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { innerPadding ->
        // Single scrollable container for the whole screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
        ) {
            // Hero Layer (Bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .graphicsLayer {
                        // Parallax effect: moves slower than the scroll
                        // 1.0f (scroll speed) - 0.3f (desired hero speed) = 0.7f compensation
                        translationY = scrollState.value * 0.7f
                    }
            ) {
                heroContent { scrollState.value }

                // Scrim: dims the header while scrolling, then blends it into the TopAppBar's
                // own color so the two surfaces read as one by the time it's opaque. Drawn on top
                // of the hero content (rather than as a colorFilter on it) so the image stays visible.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            val dimAlpha = (scrollState.value / headerHeightPx).coerceIn(0f, 0.5f)
                            drawRect(lerp(Color.Black.copy(alpha = dimAlpha), topBarContainerColor, topBarAlpha))
                        }
                )
            }

            // Sheet Layer (Top)
            // We don't use padding here because we want the content to be able to scroll OVER the hero
            Column(modifier = Modifier.fillMaxWidth()) {
                content(innerPadding)
            }
        }
    }
}

/**
 * The chip treatment every [ImmersiveDetailLayout] top-bar icon shares -- the back button and any
 * [ImmersiveDetailLayout]'s `actions` icon alike -- so the fade-in-over-the-hero-image formula exists in
 * exactly one place. Defined here rather than duplicated at each `actions` call site, the same way
 * `IconButtonDefaults`/`TopAppBarDefaults` centralize Material's own component defaults.
 */
object ImmersiveDetailLayoutDefaults {
    @Composable
    fun ActionIconButton(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String?,
        alpha: Float,
        modifier: Modifier = Modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = (1f - alpha))
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

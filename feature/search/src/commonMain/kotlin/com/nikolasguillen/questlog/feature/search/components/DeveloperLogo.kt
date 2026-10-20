package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.request.ImageRequest
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.util.LogoBackdrop
import com.nikolasguillen.questlog.core.ui.util.LogoBackdropCache
import com.nikolasguillen.questlog.core.ui.util.readableBitmaps
import com.nikolasguillen.questlog.core.ui.util.resolveLogoTileColor
import com.nikolasguillen.questlog.core.ui.util.sampleLogoBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The icon takes this share of the tile's width, so it scales with the tile instead of using a fixed size. */
private const val PLACEHOLDER_ICON_SHARE = 0.5f

/**
 * A developer's logo on a rounded tile. The size comes from [modifier] and is meant to be square.
 *
 * The logo is shown whole (never cropped) with room around it. The tile starts in the theme's tone and, once the
 * picture is loaded, moves to the logo's own edge colour, so a logo drawn on a black or coloured rectangle reads as
 * one field. A logo with a transparent edge keeps the theme's tone, or the opposite one when its artwork is too close
 * to the theme's to be read (see [resolveLogoTileColor]). The building icon stands in when there is no [imageUrl] or
 * the image fails to load; it is not drawn behind a logo, which rarely fills the tile. Decorative: the card or header
 * that holds it says who the developer is.
 */
@Composable
internal fun DeveloperLogo(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    var failed by remember(imageUrl) { mutableStateOf(false) }
    var backdrop by remember(imageUrl) {
        mutableStateOf(imageUrl?.let { LogoBackdropCache[it] } ?: LogoBackdrop.Undetermined)
    }
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.appColors
    val tileColor by animateColorAsState(
        targetValue = backdrop.resolveLogoTileColor(colors.logoTileContainerColor, colors.logoTileContrastContainerColor),
        label = "LogoTileColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(tileColor)
    ) {
        if (imageUrl == null || failed) {
            Icon(
                imageVector = Icons.Outlined.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxSize(PLACEHOLDER_ICON_SHARE)
            )
        } else {
            AsyncImage(
                // The tile colour is read from the decoded picture, which needs readable pixels.
                model = remember(imageUrl) {
                    ImageRequest.Builder(platformContext).data(imageUrl).readableBitmaps().build()
                },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                onSuccess = { state ->
                    if (LogoBackdropCache[imageUrl] == null) {
                        scope.launch {
                            val found = withContext(Dispatchers.Default) {
                                state.result.image.asPainter(platformContext).sampleLogoBackdrop()
                            }
                            LogoBackdropCache[imageUrl] = found
                            backdrop = found
                        }
                    }
                },
                modifier = Modifier
                    .matchParentSize()
                    .padding(MaterialTheme.spacing.medium)
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun DeveloperLogoWithoutImagePreview() {
    QuestLogTheme {
        DeveloperLogo(imageUrl = null, modifier = Modifier.size(96.dp))
    }
}

@QuestLogPreviews
@Composable
private fun DeveloperLogoWithImagePreview() {
    QuestLogTheme {
        // No network in a preview, so the image never loads.
        DeveloperLogo(imageUrl = "https://images.igdb.com/igdb/image/upload/t_logo_med/example.png", modifier = Modifier.size(96.dp))
    }
}

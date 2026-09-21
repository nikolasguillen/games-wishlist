package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.feature.gamedetail.R
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.trishiraj.shadowglow.shadowGlow

private val PILL_WIDTH = 220.dp
private val PILL_HEIGHT = 76.dp
private val MAIN_ACTION_SIZE = 60.dp
private val GLOW_BLUR_RADIUS = 12.dp
private val GLOW_BLUR_SPREAD = 4.dp

/**
 * A floating action pill for the Game Detail screen.
 * Contains actions to toggle favorite, manage lists, and share.
 */
@Composable
internal fun GameDetailActionPill(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onManageListClick: () -> Unit,
    onShareClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Consume clicks to prevent them from passing to elements below
            )
    ) {
        PillBackground(
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.Center)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .size(width = PILL_WIDTH, height = PILL_HEIGHT)
                .align(Alignment.Center)
                .padding(horizontal = MaterialTheme.spacing.extraLarge)
        ) {
            PillIconAction(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(R.string.favorite_content_description),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                onClick = onFavoriteClick
            )
            PillMainAction(onClick = onManageListClick)
            PillIconAction(
                icon = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share_content_description),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onShareClick
            )
        }
    }
}

/**
 * The blurred, metal-bordered surface the actions sit on. Drawn as a sibling of the action [Row]
 * so the glow is not clipped by the pill shape.
 */
@Composable
private fun PillBackground(
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val tintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .pillGlow(color = MaterialTheme.colorScheme.primary, borderRadius = PILL_HEIGHT / 2)
            .size(width = PILL_WIDTH, height = PILL_HEIGHT)
            .clip(CircleShape)
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle {
                    colorEffects(listOf(HazeColorEffect.tint(tintColor)))
                }
            )
    )
}

/**
 * A secondary, borderless action sitting on the pill surface.
 */
@Composable
private fun PillIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

/**
 * The primary action: a filled circle that overflows the pill surface.
 */
@Composable
private fun PillMainAction(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .size(MAIN_ACTION_SIZE)
            .padding(MaterialTheme.spacing.smallMedium)
            .pillGlow(color = MaterialTheme.colorScheme.primary, borderRadius = PILL_HEIGHT / 2)
    ) {
        Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, tint = Color.Black)
    }
}

/**
 * The pill's shared glow: centered, with no offset.
 */
private fun Modifier.pillGlow(color: Color, borderRadius: Dp): Modifier =
    shadowGlow(
        color = color,
        borderRadius = borderRadius,
        blurRadius = GLOW_BLUR_RADIUS,
        offsetX = 0.dp,
        offsetY = 0.dp,
        spread = GLOW_BLUR_SPREAD
    )

private val PREVIEW_WIDTH = 320.dp
private val PREVIEW_HEIGHT = 180.dp

/**
 * The pill blurs whatever a [hazeSource] captured, so the preview needs one: fake sheet content
 * (title, subtitle, card) rather than the hero image, since that's what actually sits behind the
 * pill at rest — matching it keeps the glow read against a realistic backdrop instead of an empty one.
 */
@Preview
@Composable
private fun GameDetailActionPillPreview() {
    GamesWishlistTheme {
        val hazeState = rememberHazeState()
        Box(
            modifier = Modifier
                .size(width = PREVIEW_WIDTH, height = PREVIEW_HEIGHT)
                .height(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            GameDetailActionPill(
                isFavorite = true,
                onFavoriteClick = {},
                onManageListClick = {},
                onShareClick = {},
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

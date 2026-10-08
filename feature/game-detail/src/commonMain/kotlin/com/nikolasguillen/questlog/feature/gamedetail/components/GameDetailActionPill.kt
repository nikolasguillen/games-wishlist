package com.nikolasguillen.questlog.feature.gamedetail.components

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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.isDarkTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomOutlinedIcon
import com.nikolasguillen.questlog.feature.gamedetail.resources.Res
import com.nikolasguillen.questlog.feature.gamedetail.resources.favorite_content_description
import com.nikolasguillen.questlog.feature.gamedetail.resources.share_content_description
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource

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
            // The gold favorite fill needs a defining edge only in light theme, where it can blend
            // into a bright blurred backdrop — dark theme's near-black surfaces already give it
            // enough separation.
            val isLightTheme = !MaterialTheme.isDarkTheme
            PillIconAction(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(Res.string.favorite_content_description),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                onClick = onFavoriteClick,
                outlineColor = if (isFavorite && isLightTheme) MaterialTheme.colorScheme.onSurface else null
            )
            PillMainAction(onClick = onManageListClick)
            PillIconAction(
                icon = Icons.Outlined.Share,
                contentDescription = stringResource(Res.string.share_content_description),
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
            .pillGlow(color = MaterialTheme.colorScheme.primary)
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
 * A secondary action sitting on the pill surface. When [outlineColor] is non-null, a stroke
 * tracing the icon's own silhouette is drawn behind it — see [CustomOutlinedIcon].
 */
@Composable
private fun PillIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    outlineColor: Color? = null
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
    ) {
        CustomOutlinedIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            outlineColor = outlineColor
        )
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
    ) {
        Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, tint = Color.Black)
    }
}

/**
 * The pill's shared glow: centered, with no offset.
 */
private fun Modifier.pillGlow(color: Color): Modifier =
    dropShadow(
        shape = CircleShape,
        shadow = Shadow(radius = GLOW_BLUR_RADIUS, spread = GLOW_BLUR_SPREAD, color = color)
    )

private val PREVIEW_WIDTH = 320.dp
private val PREVIEW_HEIGHT = 180.dp

/**
 * The pill blurs whatever a [hazeSource] captured, so the preview needs one: fake sheet content
 * (title, subtitle, card) rather than the hero image, since that's what actually sits behind the
 * pill at rest — matching it keeps the glow read against a realistic backdrop instead of an empty one.
 */
@QuestLogPreviews
@Composable
private fun GameDetailActionPillPreview() {
    QuestLogTheme {
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

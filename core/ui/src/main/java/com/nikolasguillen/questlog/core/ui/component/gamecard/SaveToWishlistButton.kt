package com.nikolasguillen.questlog.core.ui.component.gamecard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.R
import kotlinx.coroutines.launch

/**
 * Toggles the game's membership in the default wishlist. A tap toggles it directly; a long press
 * (shared with the card underneath it, so it fires regardless of which of the two catches the gesture)
 * opens the list selector instead.
 */
@Composable
fun SaveToWishlistButton(
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    longClickLabel: String,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    // Triggered from the click callback rather than derived from isSaved: keying it to the state would
    // replay the pulse on every card that is already saved the moment the grid first composes, and again
    // whenever a scrolled-away card re-enters composition.
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // The touch target (48dp, accessibility minimum) is kept larger than the visible circle (32dp),
    // the same way Material's own IconButton pads a 24dp icon inside a 48dp target.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    scope.launch {
                        scale.animateTo(
                            1.3f,
                            animationSpec = spring(stiffness = Spring.StiffnessHigh)
                        )
                        scale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    onSaveClick()
                },
                // combinedClickable already performs HapticFeedbackType.LongPress on its own, so the
                // long-click branch needs no explicit call.
                onLongClick = onLongClick,
                onLongClickLabel = longClickLabel
            )
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            modifier = Modifier
                .size(32.dp)
                .scale(scale.value)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isSaved) {
                            R.string.remove_from_wishlist_content_description
                        } else {
                            R.string.add_to_wishlist_content_description
                        }
                    ),
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(MaterialTheme.spacing.large)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SaveToWishlistSelectedButtonPreview() {
    QuestLogTheme {
        SaveToWishlistButton(
            isSaved = true,
            onSaveClick = {},
            onLongClick = {},
            longClickLabel = "Long click"
        )
    }
}

@Preview
@Composable
private fun SaveToWishlistUnselectedButtonPreview() {
    QuestLogTheme {
        SaveToWishlistButton(
            isSaved = false,
            onSaveClick = {},
            onLongClick = {},
            longClickLabel = "Long click"
        )
    }
}
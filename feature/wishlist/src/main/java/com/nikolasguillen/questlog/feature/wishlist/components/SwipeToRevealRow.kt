package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.feature.wishlist.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

private val RevealActionWidth = 80.dp

@Composable
internal fun SwipeToRevealRow(
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val revealWidthPx = with(LocalDensity.current) { RevealActionWidth.toPx() }
    val haptics = LocalHapticFeedback.current

    val dragState = remember(revealWidthPx) {
        AnchoredDraggableState(
            initialValue = false,
            anchors = DraggableAnchors {
                false at 0f
                true at -revealWidthPx
            }
        )
    }

    // The external state drives the animation (programmatic opening and forced closing by the caller).
    LaunchedEffect(isRevealed) {
        dragState.animateTo(isRevealed)
    }
    // The user's gesture bubbles up to the caller. drop(1) discards snapshotFlow's initial emission,
    // which only reports the starting value rather than a user choice: without it, every freshly
    // composed row would notify "closed" and reset the row that is actually open.
    LaunchedEffect(dragState) {
        snapshotFlow { dragState.settledValue }
            .drop(1)
            .collect(onRevealChange)
    }
    // Fires the moment the button is fully uncovered, while the finger is still down: unlike
    // settledValue above (which only updates once the gesture ends), the raw offset tracks the
    // drag in real time. Without an overscroll effect the drag clamps at the anchors, so the offset
    // can't go past -revealWidthPx — reaching it is exactly "fully revealed". distinctUntilChanged
    // + drop(1) fire the haptic once per reveal, not on every recomposition while held there.
    LaunchedEffect(dragState) {
        snapshotFlow { dragState.requireOffset() <= -revealWidthPx }
            .distinctUntilChanged()
            .drop(1)
            .collect { fullyRevealed ->
                if (fullyRevealed) haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
            }
    }

    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(RevealActionWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = 0.5f + revealProgress(dragState, revealWidthPx) * 0.5f
                    }
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(onClick = onRemoveClick)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.remove_game_action),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.graphicsLayer {
                        // Starts pinned to the button's trailing edge — exactly where the sliver of
                        // reveal first appears, so the icon is visible from the very first pixel of
                        // drag — and eases inward to its resting, centered spot as the reveal
                        // completes. The icon sits at the box's own center by default (contentAlignment
                        // = Center), half the box's width away from that edge, so half — not the full
                        // width — is the distance that brings it there.
                        translationX = (1f - revealProgress(dragState, revealWidthPx)) * (revealWidthPx / 2f)
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(dragState.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(state = dragState, orientation = Orientation.Horizontal)
        ) {
            content()
        }
    }
}

// Deferred to the draw phase (called from inside graphicsLayer lambdas) so reading the
// frequently-changing drag offset doesn't trigger a recomposition on every dragged pixel.
private fun revealProgress(dragState: AnchoredDraggableState<Boolean>, revealWidthPx: Float): Float =
    ((-dragState.requireOffset()) / revealWidthPx).coerceIn(0f, 1f)

@Preview(showBackground = true)
@Composable
private fun SwipeToRevealRowClosedPreview() {
    QuestLogTheme {
        Surface {
            SwipeToRevealRow(
                isRevealed = false,
                onRevealChange = {},
                onRemoveClick = {}
            ) {
                WishlistGameRow(
                    game = GameItemUiModel.getDummy(),
                    onClick = {},
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SwipeToRevealRowOpenPreview() {
    QuestLogTheme {
        Surface {
            SwipeToRevealRow(
                isRevealed = true,
                onRevealChange = {},
                onRemoveClick = {}
            ) {
                WishlistGameRow(
                    game = GameItemUiModel.getDummy(),
                    onClick = {}
                )
            }
        }
    }
}

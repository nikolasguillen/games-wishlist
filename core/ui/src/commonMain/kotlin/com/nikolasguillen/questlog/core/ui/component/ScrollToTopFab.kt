package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.scroll_to_top_content_description
import org.jetbrains.compose.resources.stringResource

/**
 * The floating up-arrow that takes a long, scrolling screen back to its top. It grows in and out as
 * [visible] changes.
 *
 * It holds no scroll state: the caller decides when it is [visible] (see
 * [com.nikolasguillen.questlog.core.ui.util.UiConstants.SCROLL_TO_TOP_AFTER_ITEM_INDEX]) and what [onClick]
 * scrolls, so every screen that uses it looks and behaves the same.
 */
@Composable
fun ScrollToTopFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CustomFab(
        onClick = onClick,
        modifier = modifier.animateFloatingActionButton(
            visible = visible,
            alignment = Alignment.Center
        )
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(Res.string.scroll_to_top_content_description)
        )
    }
}

object ScrollToTopFabDefaults {
    /**
     * Bottom content padding for the list under a [ScrollToTopFab], so the last item is never left covered
     * by the button when the list is scrolled all the way down: the 56dp button, the margin the `Scaffold`
     * keeps around it, and a little breathing room. Constant on purpose: it does not depend on whether the
     * button is showing, so the list does not jump when it appears.
     */
    val ContentBottomPadding: Dp
        @Composable get() = MaterialTheme.spacing.doubleLarge * 3
}

@QuestLogPreviews
@Composable
private fun ScrollToTopFabPreview() {
    QuestLogTheme {
        ScrollToTopFab(visible = true, onClick = {})
    }
}

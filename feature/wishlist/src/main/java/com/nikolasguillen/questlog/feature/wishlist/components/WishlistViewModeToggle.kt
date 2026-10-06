package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import com.nikolasguillen.questlog.feature.wishlist.R

/**
 * Switches the wishlist between its list and grid layouts. It shows the icon of the layout it switches
 * **to**; the content description names that action, and the state description announces the current view.
 */
@Composable
internal fun WishlistViewModeToggle(
    viewMode: WishlistViewMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGrid = viewMode == WishlistViewMode.GRID
    val currentViewDescription = stringResource(if (isGrid) R.string.grid_view_state else R.string.list_view_state)

    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = modifier.semantics { stateDescription = currentViewDescription }
    ) {
        Icon(
            imageVector = if (isGrid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
            contentDescription = stringResource(
                if (isGrid) R.string.show_as_list_action else R.string.show_as_grid_action
            )
        )
    }
}

@QuestLogPreviews
@Composable
private fun WishlistViewModeToggleListPreview() {
    QuestLogTheme {
        WishlistViewModeToggle(viewMode = WishlistViewMode.LIST, onClick = {})
    }
}

@QuestLogPreviews
@Composable
private fun WishlistViewModeToggleGridPreview() {
    QuestLogTheme {
        WishlistViewModeToggle(viewMode = WishlistViewMode.GRID, onClick = {})
    }
}

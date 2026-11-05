package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.material3.ExpandedDockedSearchBarWithGap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.PopupProperties
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchUiState

/**
 * What the search bar opens into on a window at least 840dp wide: a panel anchored under the bar, as wide as the bar,
 * floating over the page instead of replacing it. The rail and the page stay visible around it.
 *
 * It is Material's docked search bar with a gap between the bar and the panel. There is no scrim, so the popup never
 * dims or covers anything but the page under it. The panel itself is [WideSearchPanel], which has its own previews;
 * a popup cannot be previewed.
 *
 * The two confirmation dialogs of the recent searches are drawn from inside the popup, as the full-screen overlay
 * does. While one is open a click outside the popup no longer closes it ([PopupProperties.dismissOnClickOutside] is
 * off): a click inside the dialog lies outside the popup's bounds and would otherwise count as a click outside, closing
 * the panel from under the dialog.
 *
 * @param inputField The text field Material carries between the collapsed bar and this panel.
 * @param searchBarShape The shape of the collapsed bar, which the field keeps while the panel is open.
 * @param historyDialogs Which recent-search dialog is open; the screen owns it so it survives a layout switch.
 * @param highlighted The item the keyboard has highlighted, for the screen-reader announcement.
 * @param itemModifier Hover, mark and bring-into-view for each item; the layout builds it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WideSearchDropdown(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    searchBarShape: Shape,
    searchBarColors: SearchBarColors,
    uiState: SearchUiState,
    query: String,
    onActivate: (SearchDropdownItemUiModel) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    historyDialogs: SearchHistoryDialogState,
    onConfirmRemoveQuery: (String) -> Unit,
    onConfirmClearHistory: () -> Unit,
    highlighted: SearchDropdownItemUiModel? = null,
    itemModifier: (SearchDropdownItemUiModel) -> Modifier = { Modifier }
) {
    ExpandedDockedSearchBarWithGap(
        state = searchBarState,
        inputField = inputField,
        shape = searchBarShape,
        dropdownGapSize = MaterialTheme.spacing.small,
        dropdownScrimColor = Color.Unspecified,
        colors = searchBarColors.copy(containerColor = MaterialTheme.appColors.expandedSearchBarColor),
        properties = PopupProperties(
            focusable = true,
            clippingEnabled = false,
            dismissOnClickOutside = !historyDialogs.isAnyOpen
        )
    ) {
        WideSearchPanel(
            uiState = uiState,
            query = query,
            onActivate = onActivate,
            onRemoveRecentGame = onRemoveRecentGame,
            onShowRemovalDialog = { historyDialogs.queryToRemove = it },
            highlighted = highlighted,
            itemModifier = itemModifier
        )

        historyDialogs.queryToRemove?.let { queryToRemove ->
            RemoveHistoryItemDialog(
                query = queryToRemove,
                onConfirm = {
                    onConfirmRemoveQuery(queryToRemove)
                    historyDialogs.queryToRemove = null
                },
                onDismiss = { historyDialogs.queryToRemove = null }
            )
        }

        if (historyDialogs.showClearHistoryDialog) {
            ClearHistoryDialog(
                onConfirm = {
                    onConfirmClearHistory()
                    historyDialogs.showClearHistoryDialog = false
                },
                onDismiss = { historyDialogs.showClearHistoryDialog = false }
            )
        }
    }
}

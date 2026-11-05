package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Which confirmation dialog of the recent searches is open: removing one search, or clearing them all.
 *
 * The screen owns it, not the overlay or the dropdown that opens the dialogs, so a window that crosses the wide
 * threshold with a dialog open keeps it. It is composable state, saved so a rotation keeps it too.
 */
@Stable
internal class SearchHistoryDialogState(
    queryToRemove: String? = null,
    showClearHistoryDialog: Boolean = false
) {
    /** The recent search the removal dialog asks about, or null when that dialog is closed. */
    var queryToRemove by mutableStateOf(queryToRemove)

    /** Whether the "clear all" dialog is open. */
    var showClearHistoryDialog by mutableStateOf(showClearHistoryDialog)

    /** Whether either dialog is open. */
    val isAnyOpen: Boolean get() = queryToRemove != null || showClearHistoryDialog

    companion object {
        val Saver = listSaver<SearchHistoryDialogState, Any?>(
            save = { listOf(it.queryToRemove, it.showClearHistoryDialog) },
            restore = { SearchHistoryDialogState(it[0] as String?, it[1] as Boolean) }
        )
    }
}

@Composable
internal fun rememberSearchHistoryDialogState(): SearchHistoryDialogState =
    rememberSaveable(saver = SearchHistoryDialogState.Saver) { SearchHistoryDialogState() }

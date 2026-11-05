package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.feature.search.mapper.defaultBrowseGenres
import com.nikolasguillen.questlog.feature.search.mapper.toLabelUiText
import com.nikolasguillen.questlog.feature.search.model.GameSuggestionUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchHistoryUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchSuggestionsUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchUiState
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.clear_all
import com.nikolasguillen.questlog.feature.search.resources.see_all_results_for
import org.jetbrains.compose.resources.stringResource

/**
 * The content of the wide search dropdown: [WideSearchSuggestions] while a query is typed and [WideSearchHome]
 * before the first keystroke, in one column that scrolls on its own when the panel is shorter than its content.
 *
 * It always takes the full height the dropdown is allowed, so it does not change size as the content does while the
 * user types; short content leaves the rest empty.
 *
 * Nothing in it can take keyboard focus. The text field keeps focus for as long as the panel is open, and the
 * arrow keys move a highlight instead (see `SearchDropdownNavigation`); Compose would otherwise move focus onto a
 * chip or a row on a mouse press, and Tab would walk into the popup.
 *
 * @param highlighted The item the keyboard or the mouse has highlighted, if any. A screen reader announces it as it
 * changes, through an invisible live region.
 * @param itemModifier Applied to each actionable item, so the caller can mark and track it.
 */
@Composable
internal fun WideSearchPanel(
    uiState: SearchUiState,
    query: String,
    onActivate: (SearchDropdownItemUiModel) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    onShowRemovalDialog: (String) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: SearchDropdownItemUiModel? = null,
    itemModifier: (SearchDropdownItemUiModel) -> Modifier = { Modifier }
) {
    val columnModifier = modifier
        .fillMaxSize()
        .focusProperties { canFocus = false }
        .verticalScroll(rememberScrollState())
        .padding(vertical = MaterialTheme.spacing.medium)

    // Material lays chips and buttons out 48dp tall around a 32dp visible shape, and only the visible shape takes the
    // mouse. Left as is, the highlight (which wraps the layout box) would show over a strip a click does not reach, so
    // the panel's controls are laid out at their visible size and the highlight and the click area coincide.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Box {
            if (query.isNotBlank()) {
                WideSearchSuggestions(
                    query = query,
                    selectedSearchType = uiState.selectedSearchType,
                    suggestions = uiState.suggestions,
                    onActivate = onActivate,
                    modifier = columnModifier,
                    itemModifier = itemModifier
                )
            } else {
                WideSearchHome(
                    selectedSearchType = uiState.selectedSearchType,
                    browseGenres = uiState.browseGenres,
                    history = uiState.history,
                    onActivate = onActivate,
                    onRemoveRecentGame = onRemoveRecentGame,
                    onShowRemovalDialog = onShowRemovalDialog,
                    modifier = columnModifier,
                    itemModifier = itemModifier
                )
            }

            if (highlighted != null) {
                // The highlight is not focus, so a screen reader would say nothing as it moves. A polite live region
                // reads the label when it changes. It is a 1dp node rather than a zero-size one: a node with no bounds
                // can be dropped from the accessibility tree. It uses no `selected` semantics, which the type chips
                // already use for the chosen type.
                val label = highlighted.announcement(uiState = uiState, query = query)
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = label
                        }
                )
            }
        }
    }
}

/** What a screen reader says when the highlight lands on this item, from labels the panel already shows. */
@Composable
private fun SearchDropdownItemUiModel.announcement(uiState: SearchUiState, query: String): String = when (this) {
    is SearchDropdownItemUiModel.TypeChip -> type.toLabelUiText().asString()
    is SearchDropdownItemUiModel.Genre ->
        uiState.browseGenres.firstOrNull { it.id == genreId }?.label?.asString().orEmpty()
    SearchDropdownItemUiModel.ClearHistory -> stringResource(Res.string.clear_all)
    is SearchDropdownItemUiModel.RecentQuery -> this.query
    is SearchDropdownItemUiModel.HistorySuggestion -> this.query
    is SearchDropdownItemUiModel.RecentGame ->
        uiState.history.games.firstOrNull { it.id == gameId }?.name.orEmpty()
    is SearchDropdownItemUiModel.GameSuggestion ->
        uiState.suggestions.gameSuggestions.firstOrNull { it.id == gameId }?.name.orEmpty()
    SearchDropdownItemUiModel.SeeAllResults -> stringResource(Res.string.see_all_results_for, query)
}

private val previewHistory = SearchHistoryUiModel(
    queries = listOf("The Witcher", "Cyberpunk 2077", "Hollow Knight", "Elden Ring"),
    games = List(6) { index ->
        GameItemUiModel.getDummy().copy(id = index + 1, name = "Game ${index + 1}")
    }
)

private val previewSuggestions = SearchSuggestionsUiModel(
    historySuggestions = listOf("cyberpunk", "cyberpunk mods"),
    gameSuggestions = listOf(
        GameSuggestionUiModel(1, "Cyberpunk 2077", null, "CD Projekt Red · 2020"),
        GameSuggestionUiModel(2, "Cyberpunk 2077: Phantom Liberty", null, "CD Projekt Red · 2023")
    )
)

@Composable
private fun WideSearchPanelPreview(uiState: SearchUiState, query: String) {
    QuestLogTheme {
        Surface(color = MaterialTheme.appColors.expandedSearchBarColor) {
            WideSearchPanel(
                uiState = uiState,
                query = query,
                onActivate = {},
                onRemoveRecentGame = {},
                onShowRemovalDialog = {}
            )
        }
    }
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchPanelHomePreview() {
    WideSearchPanelPreview(
        uiState = SearchUiState(history = previewHistory, browseGenres = defaultBrowseGenres()),
        query = ""
    )
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchPanelTypingPreview() {
    WideSearchPanelPreview(
        uiState = SearchUiState(suggestions = previewSuggestions, browseGenres = defaultBrowseGenres()),
        query = "cyberpunk"
    )
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchPanelLoadingPreview() {
    WideSearchPanelPreview(
        uiState = SearchUiState(
            suggestions = previewSuggestions.copy(gameSuggestions = emptyList(), isLoadingRemote = true)
        ),
        query = "cyberpunk"
    )
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchPanelFirstTimePreview() {
    WideSearchPanelPreview(
        uiState = SearchUiState(browseGenres = defaultBrowseGenres()),
        query = ""
    )
}

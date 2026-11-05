package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.feature.search.model.GameSuggestionUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchSuggestionsUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchType
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.suggestions_section_games
import com.nikolasguillen.questlog.feature.search.resources.suggestions_section_recent
import org.jetbrains.compose.resources.stringResource

/**
 * What the wide search panel shows while a query is typed: the type chips, the matching recent searches, the game
 * suggestions (shimmer rows while they load) and, always last, the "See all results" row. It mirrors the overlay's
 * suggestions list condition for condition, and draws its sections in the order of `searchDropdownItems`, which
 * `SearchDropdownItemsTest` pins.
 *
 * It is a plain column, not a lazy list: there are at most three chips, five recent searches, four games and one row.
 *
 * @param itemModifier Applied to each row and to a box around each chip, so the caller can mark and track them.
 */
@Composable
internal fun WideSearchSuggestions(
    query: String,
    selectedSearchType: SearchType,
    suggestions: SearchSuggestionsUiModel,
    onActivate: (SearchDropdownItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
    itemModifier: (SearchDropdownItemUiModel) -> Modifier = { Modifier }
) {
    Column(modifier = modifier) {
        SearchTypeChipRow(
            selected = selectedSearchType,
            onSelected = { onActivate(SearchDropdownItemUiModel.TypeChip(it)) },
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            chipModifier = { itemModifier(SearchDropdownItemUiModel.TypeChip(it)) },
            modifier = Modifier
                .padding(vertical = MaterialTheme.spacing.small)
                .fillMaxWidth()
        )

        if (suggestions.historySuggestions.isNotEmpty()) {
            SuggestionSectionHeader(title = stringResource(Res.string.suggestions_section_recent))
            suggestions.historySuggestions.forEach { historyQuery ->
                HistorySuggestionRow(
                    query = historyQuery,
                    onClick = { onActivate(SearchDropdownItemUiModel.HistorySuggestion(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(itemModifier(SearchDropdownItemUiModel.HistorySuggestion(historyQuery)))
                )
            }
        }

        if (suggestions.isLoadingRemote || suggestions.gameSuggestions.isNotEmpty()) {
            SuggestionSectionHeader(title = stringResource(Res.string.suggestions_section_games))
        }

        if (suggestions.isLoadingRemote) {
            repeat(LOADING_SUGGESTION_COUNT) {
                LoadingSuggestionRow(modifier = Modifier.fillMaxWidth())
            }
        } else {
            suggestions.gameSuggestions.forEach { gameSuggestion ->
                GameSuggestionRow(
                    suggestion = gameSuggestion,
                    onClick = { onActivate(SearchDropdownItemUiModel.GameSuggestion(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(itemModifier(SearchDropdownItemUiModel.GameSuggestion(gameSuggestion.id)))
                )
            }
        }

        SeeAllResultsRow(
            query = query,
            onClick = { onActivate(SearchDropdownItemUiModel.SeeAllResults) },
            modifier = Modifier
                .fillMaxWidth()
                .then(itemModifier(SearchDropdownItemUiModel.SeeAllResults))
        )
    }
}

private val previewSuggestions = SearchSuggestionsUiModel(
    historySuggestions = listOf("cyberpunk", "cyberpunk mods", "cyberpunk 2078"),
    gameSuggestions = listOf(
        GameSuggestionUiModel(1, "Cyberpunk 2077", null, "CD Projekt Red · 2020"),
        GameSuggestionUiModel(2, "Cyberpunk 2077: Phantom Liberty", null, "CD Projekt Red · 2023"),
        GameSuggestionUiModel(3, "Cyberpunk 2077: Ultimate Edition", null, "CD Projekt Red · 2023"),
        GameSuggestionUiModel(4, "Cyberpunk: Edgerunners", null, "Studio Trigger")
    )
)

@Composable
private fun WideSearchSuggestionsPreview(
    query: String,
    suggestions: SearchSuggestionsUiModel
) {
    QuestLogTheme {
        Surface(color = MaterialTheme.appColors.expandedSearchBarColor) {
            WideSearchSuggestions(
                query = query,
                selectedSearchType = SearchType.GAMES,
                suggestions = suggestions,
                onActivate = {}
            )
        }
    }
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchSuggestionsFullPreview() {
    WideSearchSuggestionsPreview(query = "cyberpunk", suggestions = previewSuggestions)
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchSuggestionsLoadingPreview() {
    WideSearchSuggestionsPreview(
        query = "cyberpunk",
        suggestions = previewSuggestions.copy(gameSuggestions = emptyList(), isLoadingRemote = true)
    )
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchSuggestionsEmptyPreview() {
    WideSearchSuggestionsPreview(query = "zzzz", suggestions = SearchSuggestionsUiModel())
}

/** A very long query and a very long title are cut with an ellipsis and never widen the panel. */
@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchSuggestionsLongTextPreview() {
    val longTitle = "The Legend of an Extraordinarily Long Game Title That Keeps Going Well Past Any Reasonable Width"
    WideSearchSuggestionsPreview(
        query = "a query typed by someone who never stops typing and just keeps going until the row runs out of room",
        suggestions = SearchSuggestionsUiModel(
            historySuggestions = listOf(longTitle),
            gameSuggestions = listOf(GameSuggestionUiModel(1, longTitle, null, "A studio with a long name · 2020"))
        )
    )
}

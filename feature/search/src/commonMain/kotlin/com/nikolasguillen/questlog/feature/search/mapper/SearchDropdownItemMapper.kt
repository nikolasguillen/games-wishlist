package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchHistoryUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchSuggestionsUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchType

/**
 * The actionable items of the wide search panel, in the order the panel draws them, which is also the order the
 * arrow keys walk.
 *
 * With a blank [query] (empty or only spaces) the panel shows its home: the type chips, the [browseGenres], the
 * "Clear all" button (only when there are recent searches), the recent searches and the recently viewed games.
 * With a query it shows the type chips, the matching recent searches, the game suggestions and the "See all
 * results" row. While [SearchSuggestionsUiModel.isLoadingRemote] the panel shows loading rows in place of the game
 * suggestions, so none of them is an item yet.
 *
 * `WideSearchHome` and `WideSearchSuggestions` draw their sections in this same order; `SearchDropdownItemsTest`
 * pins it.
 */
internal fun searchDropdownItems(
    query: String,
    browseGenres: List<BrowseGenreUiModel>,
    history: SearchHistoryUiModel,
    suggestions: SearchSuggestionsUiModel
): List<SearchDropdownItemUiModel> = buildList {
    SearchType.entries.forEach { add(SearchDropdownItemUiModel.TypeChip(it)) }
    if (query.isBlank()) {
        browseGenres.forEach { add(SearchDropdownItemUiModel.Genre(it.id)) }
        if (history.queries.isNotEmpty()) add(SearchDropdownItemUiModel.ClearHistory)
        history.queries.forEach { add(SearchDropdownItemUiModel.RecentQuery(it)) }
        history.games.forEach { add(SearchDropdownItemUiModel.RecentGame(it.id)) }
    } else {
        suggestions.historySuggestions.forEach { add(SearchDropdownItemUiModel.HistorySuggestion(it)) }
        if (!suggestions.isLoadingRemote) {
            suggestions.gameSuggestions.forEach { add(SearchDropdownItemUiModel.GameSuggestion(it.id)) }
        }
        add(SearchDropdownItemUiModel.SeeAllResults)
    }
}

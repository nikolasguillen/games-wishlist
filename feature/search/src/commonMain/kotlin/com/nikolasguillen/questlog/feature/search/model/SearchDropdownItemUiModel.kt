package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * One actionable item of the wide search panel: something the user can highlight and activate.
 *
 * Its equality is its identity, and that is what the highlight is keyed by -- an item, never an index, so a
 * highlight follows its row when the list changes and clears when the row goes. Section headers, loading rows and
 * the empty-state message are not items: they are never highlighted.
 */
@Immutable
internal sealed interface SearchDropdownItemUiModel {

    /** A chip that picks what the search looks up. */
    data class TypeChip(val type: SearchType) : SearchDropdownItemUiModel

    /** A "Browse by genre" chip, by its IGDB genre id. */
    data class Genre(val genreId: Int) : SearchDropdownItemUiModel

    /** The "Clear all" button of the recent searches. */
    data object ClearHistory : SearchDropdownItemUiModel

    /** A recent search. */
    data class RecentQuery(val query: String) : SearchDropdownItemUiModel

    /** A recently viewed game, by id. */
    data class RecentGame(val gameId: Int) : SearchDropdownItemUiModel

    /** A recent search that matches what is being typed. */
    data class HistorySuggestion(val query: String) : SearchDropdownItemUiModel

    /** A game suggested for what is being typed, by id. */
    data class GameSuggestion(val gameId: Int) : SearchDropdownItemUiModel

    /** The "See all results for ..." row. */
    data object SeeAllResults : SearchDropdownItemUiModel
}

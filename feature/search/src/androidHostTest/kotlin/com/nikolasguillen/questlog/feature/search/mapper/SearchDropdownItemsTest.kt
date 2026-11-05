package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.GameSuggestionUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchHistoryUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchSuggestionsUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [searchDropdownItems], the rule that fixes the order of the wide search panel's items and so the order
 * the arrow keys walk: the chips, then (blank query) genres, "Clear all", recent searches and recent games, or
 * (typed query) matching recent searches, game suggestions and "See all results". It also pins that nothing is an
 * item while it is not shown: no "Clear all" without recent searches, no game suggestion while they load.
 */
class SearchDropdownItemsTest {

    private val genres = listOf(genre(12), genre(31))
    private val history = SearchHistoryUiModel(
        queries = listOf("zelda", "mario"),
        games = listOf(game(1), game(2))
    )
    private val chips = SearchType.entries.map { SearchDropdownItemUiModel.TypeChip(it) }

    @Test
    fun `an empty query lists the chips, genres, clear all, recent searches and recent games in that order`() {
        val items = searchDropdownItems("", genres, history, SearchSuggestionsUiModel())

        assertEquals(
            chips + listOf(
                SearchDropdownItemUiModel.Genre(12),
                SearchDropdownItemUiModel.Genre(31),
                SearchDropdownItemUiModel.ClearHistory,
                SearchDropdownItemUiModel.RecentQuery("zelda"),
                SearchDropdownItemUiModel.RecentQuery("mario"),
                SearchDropdownItemUiModel.RecentGame(1),
                SearchDropdownItemUiModel.RecentGame(2)
            ),
            items
        )
    }

    @Test
    fun `the type chips come in the order of the search types`() {
        val items = searchDropdownItems("", emptyList(), SearchHistoryUiModel(), SearchSuggestionsUiModel())

        assertEquals(
            listOf(SearchType.GAMES, SearchType.CHARACTERS, SearchType.DEVELOPERS),
            items.map { (it as SearchDropdownItemUiModel.TypeChip).type }
        )
    }

    @Test
    fun `a query of only spaces gives the same list as an empty one`() {
        val suggestions = SearchSuggestionsUiModel(historySuggestions = listOf("zelda"))

        assertEquals(
            searchDropdownItems("", genres, history, suggestions),
            searchDropdownItems("   ", genres, history, suggestions)
        )
    }

    @Test
    fun `no recent searches means no clear all`() {
        val items = searchDropdownItems(
            "", genres, SearchHistoryUiModel(games = listOf(game(1))), SearchSuggestionsUiModel()
        )

        assertEquals(
            chips + listOf(
                SearchDropdownItemUiModel.Genre(12),
                SearchDropdownItemUiModel.Genre(31),
                SearchDropdownItemUiModel.RecentGame(1)
            ),
            items
        )
    }

    @Test
    fun `no history and two genres gives the chips and the genres only`() {
        val items = searchDropdownItems("", genres, SearchHistoryUiModel(), SearchSuggestionsUiModel())

        assertEquals(
            chips + listOf(SearchDropdownItemUiModel.Genre(12), SearchDropdownItemUiModel.Genre(31)),
            items
        )
    }

    @Test
    fun `no genres and no history gives exactly the three chips`() {
        val items = searchDropdownItems("", emptyList(), SearchHistoryUiModel(), SearchSuggestionsUiModel())

        assertEquals(chips, items)
    }

    @Test
    fun `a typed query lists the chips, matching recent searches, game suggestions and see all results`() {
        val suggestions = SearchSuggestionsUiModel(
            historySuggestions = listOf("zelda", "zelda botw"),
            gameSuggestions = listOf(suggestion(7), suggestion(8))
        )

        val items = searchDropdownItems("ze", genres, history, suggestions)

        assertEquals(
            chips + listOf(
                SearchDropdownItemUiModel.HistorySuggestion("zelda"),
                SearchDropdownItemUiModel.HistorySuggestion("zelda botw"),
                SearchDropdownItemUiModel.GameSuggestion(7),
                SearchDropdownItemUiModel.GameSuggestion(8),
                SearchDropdownItemUiModel.SeeAllResults
            ),
            items
        )
    }

    @Test
    fun `game suggestions are not items while they are loading`() {
        val suggestions = SearchSuggestionsUiModel(
            historySuggestions = listOf("zelda"),
            gameSuggestions = listOf(suggestion(7)),
            isLoadingRemote = true
        )

        val items = searchDropdownItems("ze", genres, history, suggestions)

        assertEquals(
            chips + listOf(SearchDropdownItemUiModel.HistorySuggestion("zelda"), SearchDropdownItemUiModel.SeeAllResults),
            items
        )
    }

    @Test
    fun `a typed query with no suggestions gives the chips and see all results`() {
        val items = searchDropdownItems("ze", genres, history, SearchSuggestionsUiModel())

        assertEquals(chips + SearchDropdownItemUiModel.SeeAllResults, items)
    }

    private fun genre(id: Int) = BrowseGenreUiModel(id, UiText.DynamicString("Genre $id"))

    private fun game(id: Int) = GameItemUiModel.getDummy().copy(id = id)

    private fun suggestion(id: Int) = GameSuggestionUiModel(
        id = id,
        name = "Game $id",
        coverUrl = null,
        subtitle = "2020"
    )
}

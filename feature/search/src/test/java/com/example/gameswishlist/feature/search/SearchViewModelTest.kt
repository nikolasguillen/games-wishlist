package com.example.gameswishlist.feature.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.example.gameswishlist.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearAllHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearRecentGamesUseCase
import com.example.gameswishlist.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetRecentSearchActivityUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.example.gameswishlist.core.domain.usecase.search.RemoveRecentGameUseCase
import com.example.gameswishlist.core.domain.usecase.search.SearchGamesUseCase
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.RecentSearchActivity
import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.model.SearchResult
import com.example.gameswishlist.core.model.SearchSuggestion
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchSort
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SortingUiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [SearchGamesUseCase] and friends are one-shot suspend calls, so unlike
 * [com.example.gameswishlist.feature.gamedetail.GameDetailViewModel] there's no reactive Flow
 * from a repository to mock: state transitions are driven directly by mocked use case results.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val searchGamesUseCase = mockk<SearchGamesUseCase>()
    private val addSearchToHistoryUseCase = mockk<AddSearchToHistoryUseCase>(relaxed = true)
    private val getRecentSearchActivityUseCase = mockk<GetRecentSearchActivityUseCase>()
    private val deleteSearchHistoryItemUseCase = mockk<DeleteSearchHistoryItemUseCase>(relaxed = true)
    private val clearAllHistoryUseCase = mockk<ClearAllHistoryUseCase>(relaxed = true)
    private val removeRecentGameUseCase = mockk<RemoveRecentGameUseCase>(relaxed = true)
    private val clearRecentGamesUseCase = mockk<ClearRecentGamesUseCase>(relaxed = true)
    private val getSearchSuggestionsUseCase = mockk<GetSearchSuggestionsUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getRecentSearchActivityUseCase() } returns flowOf(RecentSearchActivity())
        coEvery { getSearchSuggestionsUseCase.getLocalSuggestions(any()) } returns emptyList()
        every { getSearchSuggestionsUseCase.getRemoteSuggestionsFlow(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testGame(
        id: Int,
        name: String = "Game $id",
        rating: Double = 0.0,
        ratingCount: Int = 0,
        platforms: List<Platform> = emptyList()
    ) = Game(id = id, name = name, rating = rating, ratingCount = ratingCount, platforms = platforms)

    private fun TestScope.createViewModel(): SearchViewModel {
        return SearchViewModel(
            searchGamesUseCase = searchGamesUseCase,
            addSearchToHistoryUseCase = addSearchToHistoryUseCase,
            getRecentSearchActivityUseCase = getRecentSearchActivityUseCase,
            deleteSearchHistoryItemUseCase = deleteSearchHistoryItemUseCase,
            clearAllHistoryUseCase = clearAllHistoryUseCase,
            removeRecentGameUseCase = removeRecentGameUseCase,
            clearRecentGamesUseCase = clearRecentGamesUseCase,
            getSearchSuggestionsUseCase = getSearchSuggestionsUseCase
        ).also { advanceUntilIdle() }
    }

    private fun SearchViewModel.successState(): SearchContentState.Success =
        uiState.value.contentState as SearchContentState.Success

    /** Mutates [SearchViewModel.textFieldState] and forces the snapshot system to notify observers. */
    private fun SearchViewModel.setQuery(query: String) {
        textFieldState.setTextAndPlaceCursorAtEnd(query)
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun `OnSearchTriggered maps a successful result into Success content sorted by relevance`() =
        runTest(testDispatcher) {
            val lowRelevance = testGame(id = 1, rating = 10.0, ratingCount = 50)
            val highRelevance = testGame(id = 2, rating = 90.0, ratingCount = 50)
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(
                    games = listOf(lowRelevance, highRelevance),
                    platforms = emptyList(),
                    genres = emptyList()
                )
            )
            val viewModel = createViewModel()

            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()

            assertEquals(listOf(2, 1), viewModel.successState().games.map { it.id })
            coVerify { addSearchToHistoryUseCase("zelda") }
        }

    @Test
    fun `OnSearchTriggered maps an empty result into Empty content`() = runTest(testDispatcher) {
        coEvery { searchGamesUseCase("noresults") } returns AppResult.success(
            SearchResult(games = emptyList(), platforms = emptyList(), genres = emptyList())
        )
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("noresults"))
        advanceUntilIdle()

        assertEquals(SearchContentState.Empty, viewModel.uiState.value.contentState)
    }

    @Test
    fun `OnSearchTriggered maps a failure into Error content`() = runTest(testDispatcher) {
        coEvery { searchGamesUseCase("boom") } returns AppResult.failure(RepositoryError.NoNetwork)
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("boom"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contentState is SearchContentState.Error)
    }

    @Test
    fun `OnFilterClick toggles the selected filter and recomputes the visible games`() = runTest(testDispatcher) {
        val pc = Platform(id = 1, name = "PC")
        val matching = testGame(id = 1, platforms = listOf(pc))
        val nonMatching = testGame(id = 2)
        coEvery { searchGamesUseCase("query") } returns AppResult.success(
            SearchResult(games = listOf(matching, nonMatching), platforms = listOf(pc), genres = emptyList())
        )
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("query"))
        advanceUntilIdle()

        val platformFilter = viewModel.successState().filters.filterIsInstance<GameFilterUiModel.Platform>().first()
        viewModel.onEvent(SearchUiEvent.OnFilterClick(platformFilter))

        assertEquals(listOf(1), viewModel.successState().games.map { it.id })
    }

    @Test
    fun `OnClearFilters resets all bottom sheet filter selections`() = runTest(testDispatcher) {
        coEvery { searchGamesUseCase("query") } returns AppResult.success(
            SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
        )
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("query"))
        advanceUntilIdle()

        viewModel.onEvent(SearchUiEvent.OnOpenFilters)
        val gameTypeFilter = viewModel.uiState.value.filtersBottomSheetState.filters.first()
        viewModel.onEvent(SearchUiEvent.OnBottomSheetFilterClick(gameTypeFilter))

        viewModel.onEvent(SearchUiEvent.OnClearFilters)

        assertTrue(viewModel.uiState.value.filtersBottomSheetState.filters.all { !it.selected })
    }

    @Test
    fun `OnApplyFilters commits the bottom sheet selection into the visible games`() = runTest(testDispatcher) {
        val pc = Platform(id = 1, name = "PC")
        val matching = testGame(id = 1, platforms = listOf(pc))
        val nonMatching = testGame(id = 2)
        coEvery { searchGamesUseCase("query") } returns AppResult.success(
            SearchResult(games = listOf(matching, nonMatching), platforms = listOf(pc), genres = emptyList())
        )
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("query"))
        advanceUntilIdle()

        viewModel.onEvent(SearchUiEvent.OnOpenFilters)
        val platformFilter =
            viewModel.uiState.value.filtersBottomSheetState.filters.filterIsInstance<GameFilterUiModel.Platform>()
                .first()
        viewModel.onEvent(SearchUiEvent.OnBottomSheetFilterClick(platformFilter))
        viewModel.onEvent(SearchUiEvent.OnApplyFilters)

        assertEquals(listOf(1), viewModel.successState().games.map { it.id })
        assertFalse(viewModel.uiState.value.filtersBottomSheetState.isVisible)
    }

    @Test
    fun `OnSortChanged re-sorts by the new criteria and toggles direction on repeated selection`() =
        runTest(testDispatcher) {
            val alpha = testGame(id = 1, name = "Alpha")
            val beta = testGame(id = 2, name = "Beta")
            coEvery { searchGamesUseCase("query") } returns AppResult.success(
                SearchResult(games = listOf(alpha, beta), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("query"))
            advanceUntilIdle()

            val nameSort = SortingUiModel(
                sortType = SearchSort.NAME, label = UiText.DynamicString("Name"),
                selected = false, descending = true
            )

            viewModel.onEvent(SearchUiEvent.OnSortChanged(nameSort))
            assertEquals(listOf(2, 1), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(nameSort))
            assertEquals(listOf(1, 2), viewModel.successState().games.map { it.id })
        }

    @Test
    fun `search history activity is reflected reactively in uiState`() = runTest(testDispatcher) {
        val activity = RecentSearchActivity(queries = listOf("zelda"), games = listOf(testGame(id = 1)))
        every { getRecentSearchActivityUseCase() } returns flowOf(activity)

        val viewModel = createViewModel()

        assertEquals(listOf("zelda"), viewModel.uiState.value.history.queries)
        assertEquals(listOf(1), viewModel.uiState.value.history.games.map { it.id })
    }

    @Test
    fun `OnClearHistory clears both search history and recently viewed games`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnClearHistory)
        advanceUntilIdle()

        coVerify { clearAllHistoryUseCase() }
        coVerify { clearRecentGamesUseCase() }
    }

    @Test
    fun `OnHistoryItemRemoved deletes the given query from history`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnHistoryItemRemoved("zelda"))
        advanceUntilIdle()

        coVerify { deleteSearchHistoryItemUseCase("zelda") }
    }

    @Test
    fun `OnRecentGameRemoved removes the given game from recently viewed`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnRecentGameRemoved(42))
        advanceUntilIdle()

        coVerify { removeRecentGameUseCase(42) }
    }

    @Test
    fun `typing a query shorter than 2 chars clears suggestions`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setQuery("z")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.suggestions.isEmpty)
    }

    @Test
    fun `typing a query shows local suggestions instantly and remote ones after the debounce`() =
        runTest(testDispatcher) {
            coEvery { getSearchSuggestionsUseCase.getLocalSuggestions("ze") } returns
                listOf(SearchSuggestion.HistorySuggestion("zelda"))
            every { getSearchSuggestionsUseCase.getRemoteSuggestionsFlow("ze") } returns
                flowOf(listOf(SearchSuggestion.GameSuggestion(testGame(id = 1, name = "Zelda"))))
            val viewModel = createViewModel()

            viewModel.setQuery("ze")
            testDispatcher.scheduler.runCurrent()

            // Local suggestions and the loading flag are set before the debounce delay elapses.
            assertEquals(listOf("zelda"), viewModel.uiState.value.suggestions.historySuggestions)
            assertTrue(viewModel.uiState.value.suggestions.isLoadingRemote)
            assertTrue(viewModel.uiState.value.suggestions.gameSuggestions.isEmpty())

            advanceUntilIdle()

            assertEquals(listOf(1), viewModel.uiState.value.suggestions.gameSuggestions.map { it.id })
            assertFalse(viewModel.uiState.value.suggestions.isLoadingRemote)
        }

    @Test
    fun `committing a search cancels an in-flight suggestions fetch even for unchanged text`() =
        runTest(testDispatcher) {
            every { getSearchSuggestionsUseCase.getRemoteSuggestionsFlow("ze") } returns
                flowOf(listOf(SearchSuggestion.GameSuggestion(testGame(id = 1, name = "Zelda"))))
            coEvery { searchGamesUseCase("ze") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 2)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()

            viewModel.setQuery("ze")
            testDispatcher.scheduler.runCurrent()

            // Commit a search for the exact same text while the debounced remote fetch is still
            // pending: textFieldState doesn't change, so only the reset trigger added in the
            // debounce fix can cancel the pending fetch before it resolves.
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("ze"))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.suggestions.isEmpty)
        }
}

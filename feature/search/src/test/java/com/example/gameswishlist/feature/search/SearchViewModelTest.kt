package com.example.gameswishlist.feature.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.example.gameswishlist.core.domain.model.WishlistAssignment
import com.example.gameswishlist.core.domain.usecase.ToggleWishlistUseCase
import com.example.gameswishlist.core.domain.usecase.discover.GetDiscoverFeedUseCase
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistedGameIdsUseCase
import com.example.gameswishlist.core.domain.usecase.list.RemoveGameFromListUseCase
import com.example.gameswishlist.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearAllHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearRecentGamesUseCase
import com.example.gameswishlist.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetRecentSearchActivityUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.example.gameswishlist.core.domain.usecase.search.RemoveRecentGameUseCase
import com.example.gameswishlist.core.domain.usecase.search.SearchGamesUseCase
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.RecentSearchActivity
import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.model.SearchResult
import com.example.gameswishlist.core.model.SearchSuggestion
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.model.DiscoverContentState
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchSort
import com.example.gameswishlist.feature.search.model.SearchUiEffect
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SortingUiModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
 * [SearchGamesUseCase] and friends are one-shot suspend calls, so most state transitions here are
 * driven directly by mocked use case results rather than by a repository Flow.
 *
 * [GetDiscoverFeedUseCase] is the exception: it returns a Flow that re-emits whenever the user changes
 * their platform selection, so the tests that care about the feed mock it with a `MutableStateFlow` and
 * push a second emission to stand in for that change. The refresh tests go one step further and answer
 * the mock with a flow built from the `refresh` argument the ViewModel actually passed, since what those
 * tests cover is that [SearchUiEvent.OnRefreshDiscover] reaches that argument -- everything about how
 * the use case itself reacts to it is [GetDiscoverFeedUseCase]'s own test's job, not this one's.
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
    private val getDiscoverFeedUseCase = mockk<GetDiscoverFeedUseCase>()
    private val getWishlistedGameIdsUseCase = mockk<GetWishlistedGameIdsUseCase>()
    private val toggleWishlistUseCase = mockk<ToggleWishlistUseCase>(relaxed = true)
    private val getWishlistAssignmentsUseCase = mockk<GetWishlistAssignmentsUseCase>()
    private val addGameToListUseCase = mockk<AddGameToListUseCase>(relaxed = true)
    private val removeGameFromListUseCase = mockk<RemoveGameFromListUseCase>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getRecentSearchActivityUseCase() } returns flowOf(RecentSearchActivity())
        coEvery { getSearchSuggestionsUseCase.getLocalSuggestions(any()) } returns emptyList()
        coEvery { getSearchSuggestionsUseCase.getRemoteSuggestions(any()) } returns emptyList()
        every { getDiscoverFeedUseCase(any()) } returns flowOf(
            AppResult.success(DiscoverFeed(popular = emptyList(), upcoming = emptyList()))
        )
        every { getWishlistedGameIdsUseCase() } returns flowOf(emptySet())
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
            getSearchSuggestionsUseCase = getSearchSuggestionsUseCase,
            getDiscoverFeedUseCase = getDiscoverFeedUseCase,
            getWishlistedGameIdsUseCase = getWishlistedGameIdsUseCase,
            toggleWishlistUseCase = toggleWishlistUseCase,
            getWishlistAssignmentsUseCase = getWishlistAssignmentsUseCase,
            addGameToListUseCase = addGameToListUseCase,
            removeGameFromListUseCase = removeGameFromListUseCase
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
    fun `the Discover feed loads into content state on init, hero split from the upcoming shelf`() =
        runTest(testDispatcher) {
            val popular = testGame(id = 1, name = "Cindergate")
            val topAnticipated = testGame(id = 2, name = "Ashborne Reverie")
            val nextAnticipated = testGame(id = 3, name = "Nightglass")
            every { getDiscoverFeedUseCase(any()) } returns flowOf(
                AppResult.success(
                    DiscoverFeed(
                        popular = listOf(popular),
                        upcoming = listOf(topAnticipated, nextAnticipated)
                    )
                )
            )

            val viewModel = createViewModel()

            val contentState = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertEquals(listOf(1), contentState.popular.map { it.id })
            assertEquals(2, contentState.hero?.id)
            assertEquals(listOf(3), contentState.upcoming.map { it.id })
        }

    @Test
    fun `a Discover feed failure lands on the feed and not on the search results`() =
        runTest(testDispatcher) {
            every { getDiscoverFeedUseCase(any()) } returns
                flowOf(AppResult.failure(RepositoryError.NoNetwork))

            val viewModel = createViewModel()

            assertTrue(viewModel.uiState.value.discover is DiscoverContentState.Error)
            // The two content areas fail independently: no search ran, so the results are still Idle.
            assertTrue(viewModel.uiState.value.contentState is SearchContentState.Idle)
        }

    @Test
    fun `a later feed emission refreshes the screen while Discover is showing`() = runTest(testDispatcher) {
        // What the platform picker looks like from here: the use case re-emits, the screen follows.
        val feed = MutableStateFlow(
            AppResult.success(DiscoverFeed(popular = listOf(testGame(id = 1)), upcoming = emptyList()))
        )
        every { getDiscoverFeedUseCase(any()) } returns feed
        val viewModel = createViewModel()

        feed.value = AppResult.success(
            DiscoverFeed(popular = listOf(testGame(id = 5)), upcoming = emptyList())
        )
        advanceUntilIdle()

        val contentState = viewModel.uiState.value.discover as DiscoverContentState.Content
        assertEquals(listOf(5), contentState.popular.map { it.id })
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
    fun `OnRetrySearch re-runs the query still held by the text field`() = runTest(testDispatcher) {
        coEvery { searchGamesUseCase("boom") } returnsMany listOf(
            AppResult.failure(RepositoryError.NoNetwork),
            AppResult.success(
                SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
            )
        )
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("boom"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.contentState is SearchContentState.Error)

        viewModel.onEvent(SearchUiEvent.OnRetrySearch)
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.successState().games.map { it.id })
        coVerify(exactly = 2) { searchGamesUseCase("boom") }
    }

    @Test
    fun `a search committed before the Discover fetch resolves cannot be clobbered by it later`() =
        runTest(testDispatcher) {
            val discoverDeferred = CompletableDeferred<AppResult<DiscoverFeed>>()
            every { getDiscoverFeedUseCase(any()) } returns flow { emit(discoverDeferred.await()) }
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()

            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.contentState is SearchContentState.Success)

            // Resolves after the search already landed. The feed collection is deliberately still
            // running -- it has to be, to keep noticing platform changes -- and it cannot clobber the
            // results because the two no longer share a slot.
            discoverDeferred.complete(
                AppResult.success(DiscoverFeed(popular = listOf(testGame(id = 9)), upcoming = emptyList()))
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.contentState is SearchContentState.Success)
            // ...and it was not merely dropped: it landed in its own slot.
            val feed = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertEquals(listOf(9), feed.popular.map { it.id })
        }

    @Test
    fun `OnClearSearch hands the screen back to the feed without re-fetching`() = runTest(testDispatcher) {
        val popular = testGame(id = 1, name = "Cindergate")
        every { getDiscoverFeedUseCase(any()) } returns flowOf(
            AppResult.success(DiscoverFeed(popular = listOf(popular), upcoming = emptyList()))
        )
        coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
            SearchResult(games = listOf(testGame(id = 2)), platforms = emptyList(), genres = emptyList())
        )
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
        advanceUntilIdle()

        viewModel.onEvent(SearchUiEvent.OnClearSearch)
        advanceUntilIdle()

        // Idle is what puts the feed back on screen -- there is no separate "showing" flag.
        assertTrue(viewModel.uiState.value.contentState is SearchContentState.Idle)
        val feed = viewModel.uiState.value.discover as DiscoverContentState.Content
        assertEquals(listOf(1), feed.popular.map { it.id })
        assertEquals("", viewModel.textFieldState.text.toString())
        verify(exactly = 1) { getDiscoverFeedUseCase(any()) }
    }

    @Test
    fun `a feed that lands while a search is on screen is kept and shown on clear`() =
        runTest(testDispatcher) {
            val discoverDeferred = CompletableDeferred<AppResult<DiscoverFeed>>()
            every { getDiscoverFeedUseCase(any()) } returns flow { emit(discoverDeferred.await()) }
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 2)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()
            // The search commits before the feed has resolved, so the feed lands with the results on
            // screen. It used to be cancelled at this point and had to be re-fetched on clear.
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.contentState is SearchContentState.Success)

            discoverDeferred.complete(
                AppResult.success(DiscoverFeed(popular = listOf(testGame(id = 7)), upcoming = emptyList()))
            )
            advanceUntilIdle()

            viewModel.onEvent(SearchUiEvent.OnClearSearch)
            advanceUntilIdle()

            val restored = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertEquals(listOf(7), restored.popular.map { it.id })
            verify(exactly = 1) { getDiscoverFeedUseCase(any()) }
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
    fun `OnClearActiveFilters restores the full result list and deselects every filter`() =
        runTest(testDispatcher) {
            val pc = Platform(id = 1, name = "PC")
            val matching = testGame(id = 1, platforms = listOf(pc))
            val nonMatching = testGame(id = 2)
            coEvery { searchGamesUseCase("query") } returns AppResult.success(
                SearchResult(games = listOf(matching, nonMatching), platforms = listOf(pc), genres = emptyList())
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("query"))
            advanceUntilIdle()

            val platformFilter =
                viewModel.successState().filters.filterIsInstance<GameFilterUiModel.Platform>().first()
            viewModel.onEvent(SearchUiEvent.OnFilterClick(platformFilter))
            assertEquals(listOf(1), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnClearActiveFilters)

            assertEquals(listOf(1, 2), viewModel.successState().games.map { it.id })
            assertTrue(viewModel.successState().filters.all { !it.selected })
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
    fun `typing a query shorter than 3 chars clears suggestions`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.setQuery("ze")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.suggestions.isEmpty)
    }

    @Test
    fun `typing a query shows local suggestions instantly and remote ones after the debounce`() =
        runTest(testDispatcher) {
            coEvery { getSearchSuggestionsUseCase.getLocalSuggestions("zel") } returns
                listOf(SearchSuggestion.HistorySuggestion("zelda"))
            coEvery { getSearchSuggestionsUseCase.getRemoteSuggestions("zel") } returns
                listOf(SearchSuggestion.GameSuggestion(testGame(id = 1, name = "Zelda II")))
            val viewModel = createViewModel()

            viewModel.setQuery("zel")
            testDispatcher.scheduler.runCurrent()

            // Local suggestions land before the debounce delay elapses, but nothing is loading yet:
            // there is no request until the delay is over.
            assertEquals(listOf("zelda"), viewModel.uiState.value.suggestions.historySuggestions)
            assertFalse(viewModel.uiState.value.suggestions.isLoadingRemote)
            assertTrue(viewModel.uiState.value.suggestions.gameSuggestions.isEmpty())

            advanceUntilIdle()

            assertEquals(listOf(1), viewModel.uiState.value.suggestions.gameSuggestions.map { it.id })
            assertFalse(viewModel.uiState.value.suggestions.isLoadingRemote)
        }

    @Test
    fun `a recent query is kept even when a game suggestion carries the same text`() =
        runTest(testDispatcher) {
            coEvery { getSearchSuggestionsUseCase.getLocalSuggestions("zel") } returns listOf(
                SearchSuggestion.HistorySuggestion("zelda"),
                SearchSuggestion.HistorySuggestion("zelda mods")
            )
            coEvery { getSearchSuggestionsUseCase.getRemoteSuggestions("zel") } returns
                listOf(SearchSuggestion.GameSuggestion(testGame(id = 1, name = "Zelda")))
            val viewModel = createViewModel()

            viewModel.setQuery("zel")
            advanceUntilIdle()

            assertEquals(
                listOf("zelda", "zelda mods"),
                viewModel.uiState.value.suggestions.historySuggestions
            )
            assertEquals(listOf(1), viewModel.uiState.value.suggestions.gameSuggestions.map { it.id })
        }

    @Test
    fun `committing a search cancels an in-flight suggestions fetch even for unchanged text`() =
        runTest(testDispatcher) {
            coEvery { getSearchSuggestionsUseCase.getRemoteSuggestions("zel") } returns
                listOf(SearchSuggestion.GameSuggestion(testGame(id = 1, name = "Zelda")))
            coEvery { searchGamesUseCase("zel") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 2)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()

            viewModel.setQuery("zel")
            testDispatcher.scheduler.runCurrent()

            // Commit a search for the exact same text while the debounced remote fetch is still
            // pending: textFieldState doesn't change, so only the reset trigger added in the
            // debounce fix can cancel the pending fetch before it resolves.
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zel"))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.suggestions.isEmpty)
        }

    @Test
    fun `a feed flagged stale by the use case is surfaced as isStale in content state`() =
        runTest(testDispatcher) {
            every { getDiscoverFeedUseCase(any()) } returns flowOf(
                AppResult.success(
                    DiscoverFeed(popular = emptyList(), upcoming = emptyList(), hasStaleRecommendations = true)
                )
            )

            val viewModel = createViewModel()

            val contentState = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertTrue(contentState.isStale)
        }

    @Test
    fun `OnRefreshDiscover marks the feed refreshing immediately, before the use case reacts`() =
        runTest(testDispatcher) {
            every { getDiscoverFeedUseCase(any()) } returns flowOf(
                AppResult.success(
                    DiscoverFeed(popular = emptyList(), upcoming = emptyList(), hasStaleRecommendations = true)
                )
            )
            val viewModel = createViewModel()

            viewModel.onEvent(SearchUiEvent.OnRefreshDiscover)

            // No advanceUntilIdle() here: isRefreshing is a direct state update the event applies
            // synchronously, not something that waits on a coroutine to pick the trigger up.
            val contentState = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertTrue(contentState.isRefreshing)
        }

    @Test
    fun `OnRefreshDiscover reaches the refresh flow the use case was invoked with`() =
        runTest(testDispatcher) {
            val refreshedFeed = AppResult.success(
                DiscoverFeed(popular = listOf(testGame(id = 5)), upcoming = emptyList())
            )
            // Stands in for the real use case reacting to its `refresh` parameter: the first collector
            // sees the initial feed, then one more per refresh emission -- exactly the contract
            // GetDiscoverFeedUseCaseTest verifies against the real implementation.
            every { getDiscoverFeedUseCase(any()) } answers {
                val refresh = firstArg<Flow<Unit>>()
                flow {
                    emit(AppResult.success(DiscoverFeed(popular = listOf(testGame(id = 1)), upcoming = emptyList())))
                    refresh.collect { emit(refreshedFeed) }
                }
            }
            val viewModel = createViewModel()

            viewModel.onEvent(SearchUiEvent.OnRefreshDiscover)
            advanceUntilIdle()

            val contentState = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertEquals(listOf(5), contentState.popular.map { it.id })
            assertFalse(contentState.isRefreshing)
        }

    @Test
    fun `OnRetryDiscover moves the feed to Loading immediately, then re-fetches it`() =
        runTest(testDispatcher) {
            every { getDiscoverFeedUseCase(any()) } answers {
                val refresh = firstArg<Flow<Unit>>()
                flow {
                    emit(AppResult.failure(RepositoryError.NoNetwork))
                    refresh.collect {
                        emit(
                            AppResult.success(
                                DiscoverFeed(popular = listOf(testGame(id = 1)), upcoming = emptyList())
                            )
                        )
                    }
                }
            }
            val viewModel = createViewModel()
            assertTrue(viewModel.uiState.value.discover is DiscoverContentState.Error)

            viewModel.onEvent(SearchUiEvent.OnRetryDiscover)

            // No advanceUntilIdle() here: the state flips to Loading synchronously, before the use
            // case reacts to the retry trigger -- the same reasoning as OnRefreshDiscover's own test.
            assertTrue(viewModel.uiState.value.discover is DiscoverContentState.Loading)

            advanceUntilIdle()
            val contentState = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertEquals(listOf(1), contentState.popular.map { it.id })
        }

    @Test
    fun `OnDismissDiscoverRefresh clears isStale without touching the rest of the feed`() =
        runTest(testDispatcher) {
            every { getDiscoverFeedUseCase(any()) } returns flowOf(
                AppResult.success(
                    DiscoverFeed(
                        popular = listOf(testGame(id = 1)),
                        upcoming = emptyList(),
                        hasStaleRecommendations = true
                    )
                )
            )
            val viewModel = createViewModel()

            viewModel.onEvent(SearchUiEvent.OnDismissDiscoverRefresh)

            val contentState = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertFalse(contentState.isStale)
            assertEquals(listOf(1), contentState.popular.map { it.id })
        }

    @Test
    fun `OnToggleSave on an unsaved game toggles it and confirms the addition by name`() =
        runTest(testDispatcher) {
            val game = testGame(id = 1, name = "Elden Ring")
            coEvery { searchGamesUseCase("elden") } returns AppResult.success(
                SearchResult(games = listOf(game), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("elden"))
            advanceUntilIdle()

            val effects = mutableListOf<SearchUiEffect>()
            val collectJob = launch { viewModel.uiEffect.toList(effects) }

            viewModel.onEvent(SearchUiEvent.OnToggleSave(1))
            advanceUntilIdle()

            coVerify { toggleWishlistUseCase(game) }
            val effect = effects.single() as SearchUiEffect.ShowSnackbar
            assertEquals(UiText.StringResource(R.string.added_to_wishlist, "Elden Ring"), effect.message)
            collectJob.cancel()
        }

    @Test
    fun `OnToggleSave on a saved game toggles it and confirms the removal by name`() =
        runTest(testDispatcher) {
            val game = testGame(id = 1, name = "Elden Ring")
            coEvery { searchGamesUseCase("elden") } returns AppResult.success(
                SearchResult(games = listOf(game), platforms = emptyList(), genres = emptyList())
            )
            every { getWishlistedGameIdsUseCase() } returns flowOf(setOf(1))
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("elden"))
            advanceUntilIdle()

            val effects = mutableListOf<SearchUiEffect>()
            val collectJob = launch { viewModel.uiEffect.toList(effects) }

            viewModel.onEvent(SearchUiEvent.OnToggleSave(1))
            advanceUntilIdle()

            coVerify { toggleWishlistUseCase(game) }
            val effect = effects.single() as SearchUiEffect.ShowSnackbar
            assertEquals(UiText.StringResource(R.string.removed_from_wishlist, "Elden Ring"), effect.message)
            collectJob.cancel()
        }

    @Test
    fun `a wishlisted-ids emission patches isSaved on the results already on screen`() =
        runTest(testDispatcher) {
            val ids = MutableStateFlow<Set<Int>>(emptySet())
            every { getWishlistedGameIdsUseCase() } returns ids
            coEvery { searchGamesUseCase("elden") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("elden"))
            advanceUntilIdle()
            assertFalse(viewModel.successState().games.single().isSaved)

            ids.value = setOf(1)
            advanceUntilIdle()

            assertTrue(viewModel.successState().games.single().isSaved)
        }

    @Test
    fun `OnToggleSave for an id missing from allGames does nothing`() = runTest(testDispatcher) {
        coEvery { searchGamesUseCase("elden") } returns AppResult.success(
            SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
        )
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("elden"))
        advanceUntilIdle()

        val effects = mutableListOf<SearchUiEffect>()
        val collectJob = launch { viewModel.uiEffect.toList(effects) }

        viewModel.onEvent(SearchUiEvent.OnToggleSave(999))
        advanceUntilIdle()

        coVerify(exactly = 0) { toggleWishlistUseCase(any()) }
        assertTrue(effects.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `OnConfirmListSelection adds only newly selected lists and removes only deselected ones`() =
        runTest(testDispatcher) {
            coEvery { searchGamesUseCase("elden") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
            )
            val listA = WishlistList(id = 10, name = "Wishlist")
            val listB = WishlistList(id = 20, name = "Backlog")
            val listC = WishlistList(id = 30, name = "Completed")
            every { getWishlistAssignmentsUseCase(1) } returns flowOf(
                listOf(
                    WishlistAssignment(listA, isAssigned = true),
                    WishlistAssignment(listB, isAssigned = false),
                    WishlistAssignment(listC, isAssigned = false)
                )
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("elden"))
            advanceUntilIdle()

            viewModel.onEvent(SearchUiEvent.OnOpenListSelector(1))
            advanceUntilIdle()

            // Select Backlog, deselect Wishlist; Completed is left untouched.
            viewModel.onEvent(SearchUiEvent.OnToggleListSelection(listB.id))
            viewModel.onEvent(SearchUiEvent.OnToggleListSelection(listA.id))
            viewModel.onEvent(SearchUiEvent.OnConfirmListSelection)
            advanceUntilIdle()

            coVerify(exactly = 1) { addGameToListUseCase(1, listB.id) }
            coVerify(exactly = 0) { addGameToListUseCase(1, listA.id) }
            coVerify(exactly = 0) { addGameToListUseCase(1, listC.id) }
            coVerify(exactly = 1) { removeGameFromListUseCase(1, listA.id) }
            coVerify(exactly = 0) { removeGameFromListUseCase(1, listB.id) }
            coVerify(exactly = 0) { removeGameFromListUseCase(1, listC.id) }
            assertEquals(null, viewModel.uiState.value.listSelectorState)
        }
}

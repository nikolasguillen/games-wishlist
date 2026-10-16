package com.nikolasguillen.questlog.feature.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import com.nikolasguillen.questlog.core.domain.model.WishlistAssignment
import com.nikolasguillen.questlog.core.domain.usecase.ToggleWishlistUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetDiscoverFeedUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.AddGameToListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistedGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.BrowseGenreUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.ClearAllHistoryUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.ClearRecentGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetBrowseGenresUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetRecentSearchActivityUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.RemoveRecentGameUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.SearchGamesUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.DiscoverFeed
import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.model.RecentSearchActivity
import com.nikolasguillen.questlog.core.model.RecommendedShelf
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.model.SearchResult
import com.nikolasguillen.questlog.core.model.SearchSuggestion
import com.nikolasguillen.questlog.core.model.ShelfReason
import com.nikolasguillen.questlog.core.model.WishlistList
import com.nikolasguillen.questlog.core.ui.resources.genre_adventure
import com.nikolasguillen.questlog.core.ui.resources.sort_relevance
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.mapper.toPlaceholder
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.DiscoverContentState
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfKey
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.model.GameFilterUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchBrowseUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchContentState
import com.nikolasguillen.questlog.feature.search.model.SearchSort
import com.nikolasguillen.questlog.feature.search.model.SearchUiEffect
import com.nikolasguillen.questlog.feature.search.model.SearchUiEvent
import com.nikolasguillen.questlog.feature.search.model.SortingUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.added_to_wishlist
import com.nikolasguillen.questlog.feature.search.resources.removed_from_wishlist
import com.nikolasguillen.questlog.feature.search.resources.sort_top_picks
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

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
    private val browseGenreUseCase = mockk<BrowseGenreUseCase>()
    private val getBrowseGenresUseCase = mockk<GetBrowseGenresUseCase>()
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
        every { getBrowseGenresUseCase() } returns flowOf(defaultGenres)
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
        platforms: List<Platform> = emptyList(),
        genres: List<Genre> = emptyList()
    ) = Game(
        id = id,
        name = name,
        rating = rating,
        ratingCount = ratingCount,
        platforms = platforms,
        genres = genres
    )

    private val defaultGenres = listOf(
        Genre(12, "Role-playing (RPG)"),
        Genre(5, "Shooter"),
        Genre(31, "Adventure"),
        Genre(15, "Strategy"),
        Genre(32, "Indie")
    )

    private val rpgChip = BrowseGenreUiModel(12, UiText.DynamicString("RPG"))
    private val indieChip = BrowseGenreUiModel(32, UiText.DynamicString("Indie"))

    private fun givenBrowse(genreId: Int, vararg games: Game, genres: List<Genre> = emptyList()) {
        coEvery { browseGenreUseCase(genreId) } returns AppResult.success(
            SearchResult(games = games.toList(), platforms = emptyList(), genres = genres)
        )
    }

    private fun SearchViewModel.sortOption(sortType: SearchSort): SortingUiModel =
        uiState.value.sortBottomSheetState.sorting.first { it.sortType == sortType }

    /** The genre of the active browse, or `null` when none is active or the browse is not a genre one. */
    private fun SearchViewModel.activeGenre(): BrowseGenreUiModel? =
        (uiState.value.activeBrowse as? SearchBrowseUiModel.Genre)?.genre

    private fun TestScope.createViewModel(): SearchViewModel {
        return SearchViewModel(
            searchGamesUseCase = searchGamesUseCase,
            browseGenreUseCase = browseGenreUseCase,
            getBrowseGenresUseCase = getBrowseGenresUseCase,
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
            assertEquals(listOf(1), contentState.popular?.games?.map { it.id })
            assertEquals(2, contentState.hero?.id)
            assertEquals(listOf(3), contentState.upcoming?.games?.map { it.id })
        }

    @Test
    fun `discover cards are marked saved from the ids cached before the feed lands`() = runTest(testDispatcher) {
        every { getWishlistedGameIdsUseCase() } returns flowOf(setOf(1))
        every { getDiscoverFeedUseCase(any()) } returns flowOf(
            AppResult.success(DiscoverFeed(popular = listOf(testGame(id = 1)), upcoming = emptyList()))
        )

        val viewModel = createViewModel()

        val content = viewModel.uiState.value.discover as DiscoverContentState.Content
        assertTrue(content.popular!!.games.single().isSaved)
    }

    @Test
    fun `the Discover hero is marked saved from the ids cached before the feed lands`() = runTest(testDispatcher) {
        every { getWishlistedGameIdsUseCase() } returns flowOf(setOf(2))
        every { getDiscoverFeedUseCase(any()) } returns flowOf(
            AppResult.success(DiscoverFeed(popular = emptyList(), upcoming = listOf(testGame(id = 2))))
        )

        val viewModel = createViewModel()

        val content = viewModel.uiState.value.discover as DiscoverContentState.Content
        assertTrue(content.hero!!.isSaved)
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
        assertEquals(listOf(5), contentState.popular?.games?.map { it.id })
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
            assertEquals(listOf(9), feed.popular?.games?.map { it.id })
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
        assertEquals(listOf(1), feed.popular?.games?.map { it.id })
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
            assertEquals(listOf(7), restored.popular?.games?.map { it.id })
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
            assertEquals(listOf(5), contentState.popular?.games?.map { it.id })
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
            assertEquals(listOf(1), contentState.popular?.games?.map { it.id })
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
            assertEquals(listOf(1), contentState.popular?.games?.map { it.id })
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
            assertEquals(UiText.StringResource(Res.string.added_to_wishlist, "Elden Ring"), effect.message)
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
            assertEquals(UiText.StringResource(Res.string.removed_from_wishlist, "Elden Ring"), effect.message)
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
                    WishlistAssignment(listA, isAssigned = true, isDefault = false),
                    WishlistAssignment(listB, isAssigned = false, isDefault = false),
                    WishlistAssignment(listC, isAssigned = false, isDefault = false)
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

            coVerify(exactly = 1) { addGameToListUseCase(match { it.id == 1 }, listB.id) }
            coVerify(exactly = 0) { addGameToListUseCase(match { it.id == 1 }, listA.id) }
            coVerify(exactly = 0) { addGameToListUseCase(match { it.id == 1 }, listC.id) }
            coVerify(exactly = 1) { removeGameFromListUseCase(1, listA.id) }
            coVerify(exactly = 0) { removeGameFromListUseCase(1, listB.id) }
            coVerify(exactly = 0) { removeGameFromListUseCase(1, listC.id) }
            assertEquals(null, viewModel.uiState.value.listSelectorState)
        }
    // region Genre browse

    @Test
    fun `the chip row starts as the five default genres in order`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(listOf(12, 5, 31, 15, 32), viewModel.uiState.value.browseGenres.map { it.id })
    }

    @Test
    fun `a genre browse shows Loading, then the games, with the browsed genre out of the filters`() =
        runTest(testDispatcher) {
            val deferred = CompletableDeferred<AppResult<SearchResult>>()
            coEvery { browseGenreUseCase(12) } coAnswers { deferred.await() }
            val viewModel = createViewModel()

            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            assertEquals(rpgChip, viewModel.activeGenre())
            assertEquals(SearchContentState.Loading, viewModel.uiState.value.contentState)

            deferred.complete(
                AppResult.success(
                    SearchResult(
                        games = listOf(testGame(id = 1, genres = listOf(Genre(12, "RPG"), Genre(32, "Indie")))),
                        platforms = emptyList(),
                        genres = listOf(Genre(32, "Indie"))
                    )
                )
            )
            advanceUntilIdle()

            val genreFilterIds = viewModel.successState().filters
                .filterIsInstance<GameFilterUiModel.Genre>().map { it.id }
            assertEquals(listOf(32), genreFilterIds)
            assertEquals(rpgChip, viewModel.activeGenre())
        }

    @Test
    fun `starting a genre browse empties the text field`() = runTest(testDispatcher) {
        givenBrowse(12, testGame(id = 1))
        val viewModel = createViewModel()
        viewModel.setQuery("zelda")

        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()

        assertEquals("", viewModel.textFieldState.text.toString())
    }

    @Test
    fun `Top picks keeps the server order and ascending reverses it, even after a filter`() =
        runTest(testDispatcher) {
            val pc = Platform(id = 1, name = "PC")
            // Deliberately neither alphabetical nor by rating: only the server's order explains [3, 1, 2, 4].
            coEvery { browseGenreUseCase(12) } returns AppResult.success(
                SearchResult(
                    games = listOf(
                        testGame(id = 3, name = "Charlie", rating = 10.0, platforms = listOf(pc)),
                        testGame(id = 1, name = "Alpha", rating = 90.0, platforms = listOf(pc)),
                        testGame(id = 2, name = "Bravo", rating = 50.0),
                        testGame(id = 4, name = "Delta", rating = 70.0, platforms = listOf(pc))
                    ),
                    platforms = listOf(pc),
                    genres = emptyList()
                )
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()

            assertEquals(listOf(3, 1, 2, 4), viewModel.successState().games.map { it.id })
            assertEquals(listOf(3, 1, 2, 4), viewModel.successState().allGames.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RELEVANCE)))
            assertEquals(listOf(4, 2, 1, 3), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RELEVANCE)))
            val platformFilter =
                viewModel.successState().filters.filterIsInstance<GameFilterUiModel.Platform>().first()
            viewModel.onEvent(SearchUiEvent.OnFilterClick(platformFilter))
            assertEquals(listOf(3, 1, 4), viewModel.successState().games.map { it.id })
        }

    @Test
    fun `Name, Rating and Release date sort a genre browse as they sort a text search`() =
        runTest(testDispatcher) {
            coEvery { browseGenreUseCase(12) } returns AppResult.success(
                SearchResult(
                    games = listOf(
                        testGame(id = 3, name = "Charlie", rating = 10.0, ratingCount = 5).copy(releaseDate = "2024-01-01"),
                        testGame(id = 1, name = "Alpha", rating = 90.0, ratingCount = 5).copy(releaseDate = "2025-06-01"),
                        testGame(id = 2, name = "Bravo", rating = 50.0, ratingCount = 5).copy(releaseDate = "2023-03-01")
                    ),
                    platforms = emptyList(),
                    genres = emptyList()
                )
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.NAME)))
            assertEquals(listOf(3, 2, 1), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RATING)))
            assertEquals(listOf(1, 2, 3), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RELEASE_DATE)))
            assertEquals(listOf(1, 3, 2), viewModel.successState().games.map { it.id })
        }

    @Test
    fun `a failed genre browse shows Error and Retry re-runs the same genre, never a text search`() =
        runTest(testDispatcher) {
            coEvery { browseGenreUseCase(12) } returnsMany listOf(
                AppResult.failure(RepositoryError.NoNetwork),
                AppResult.success(
                    SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
                )
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.contentState is SearchContentState.Error)

            viewModel.onEvent(SearchUiEvent.OnRetrySearch)
            advanceUntilIdle()

            assertEquals(listOf(1), viewModel.successState().games.map { it.id })
            assertEquals(rpgChip, viewModel.activeGenre())
            coVerify(exactly = 2) { browseGenreUseCase(12) }
            coVerify(exactly = 0) { searchGamesUseCase(any()) }
        }

    @Test
    fun `a genre browse with no games shows Empty and keeps the genre`() = runTest(testDispatcher) {
        givenBrowse(30)
        val viewModel = createViewModel()
        val pinball = BrowseGenreUiModel(30, UiText.DynamicString("Pinball"))

        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(pinball))
        advanceUntilIdle()

        assertEquals(SearchContentState.Empty, viewModel.uiState.value.contentState)
        assertEquals(pinball, viewModel.activeGenre())
    }

    @Test
    fun `two genres tapped in a row leave only the second one's results`() = runTest(testDispatcher) {
        val first = CompletableDeferred<AppResult<SearchResult>>()
        coEvery { browseGenreUseCase(12) } coAnswers { first.await() }
        givenBrowse(32, testGame(id = 2))
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(indieChip))
        advanceUntilIdle()
        // The first request is slow: whatever it carries must not land on top of the second.
        first.complete(
            AppResult.success(
                SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
            )
        )
        advanceUntilIdle()

        assertEquals(indieChip, viewModel.activeGenre())
        assertEquals(listOf(2), viewModel.successState().games.map { it.id })
    }

    @Test
    fun `the chip row follows the genres the use case emits, mapped by id`() = runTest(testDispatcher) {
        val emissions = MutableStateFlow(defaultGenres)
        every { getBrowseGenresUseCase() } returns emissions
        val viewModel = createViewModel()

        emissions.value = listOf(
            Genre(9999, "Wonder Genre"), Genre(31, "Adventure"), Genre(12, "Role-playing (RPG)"),
            Genre(5, "Shooter"), Genre(15, "Strategy")
        )
        advanceUntilIdle()

        val chips = viewModel.uiState.value.browseGenres
        assertEquals(listOf(9999, 31, 12, 5, 15), chips.map { it.id })
        assertEquals(UiText.DynamicString("Wonder Genre"), chips.first().label)
        assertEquals(
            UiText.StringResource(CoreUiRes.string.genre_adventure),
            chips[1].label
        )

        emissions.value = defaultGenres
        advanceUntilIdle()

        assertEquals(listOf(12, 5, 31, 15, 32), viewModel.uiState.value.browseGenres.map { it.id })
    }

    @Test
    fun `OnClearSearch during a genre browse returns to Discover with no active genre`() =
        runTest(testDispatcher) {
            givenBrowse(12, testGame(id = 1))
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()

            viewModel.onEvent(SearchUiEvent.OnClearSearch)

            assertEquals(null, viewModel.activeGenre())
            assertEquals(SearchContentState.Idle, viewModel.uiState.value.contentState)
        }

    @Test
    fun `a submitted query during a genre browse replaces it with a text search`() =
        runTest(testDispatcher) {
            givenBrowse(12, testGame(id = 1))
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 7)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()

            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()

            assertEquals(null, viewModel.activeGenre())
            assertEquals(listOf(7), viewModel.successState().games.map { it.id })
            coVerify(exactly = 1) { searchGamesUseCase("zelda") }
        }

    @Test
    fun `typing during a genre browse leaves it on screen, even if the text is deleted again`() =
        runTest(testDispatcher) {
            givenBrowse(12, testGame(id = 1))
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()
            val before = viewModel.uiState.value.contentState

            viewModel.setQuery("ze")
            advanceUntilIdle()
            assertEquals(rpgChip, viewModel.activeGenre())
            assertEquals(before, viewModel.uiState.value.contentState)

            viewModel.setQuery("")
            advanceUntilIdle()
            assertEquals(rpgChip, viewModel.activeGenre())
            assertEquals(before, viewModel.uiState.value.contentState)
        }

    @Test
    fun `a genre tapped while text results are showing replaces them and empties the field`() =
        runTest(testDispatcher) {
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 7)), platforms = emptyList(), genres = emptyList())
            )
            givenBrowse(12, testGame(id = 1))
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()
            assertEquals("zelda", viewModel.textFieldState.text.toString())

            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()

            assertEquals(listOf(1), viewModel.successState().games.map { it.id })
            assertEquals("", viewModel.textFieldState.text.toString())
            assertEquals(rpgChip, viewModel.activeGenre())
        }

    @Test
    fun `tapping the genre already being browsed ends in the same results`() = runTest(testDispatcher) {
        givenBrowse(12, testGame(id = 1), testGame(id = 2))
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()
        val first = viewModel.successState().games.map { it.id }

        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()

        assertEquals(first, viewModel.successState().games.map { it.id })
        assertEquals(rpgChip, viewModel.activeGenre())
    }

    @Test
    fun `a blank submit during a genre browse changes nothing`() = runTest(testDispatcher) {
        givenBrowse(12, testGame(id = 1))
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()
        val contentBefore = viewModel.uiState.value.contentState
        val sortingBefore = viewModel.uiState.value.sortBottomSheetState.sorting

        viewModel.onEvent(SearchUiEvent.OnSearchTriggered(""))
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("   "))
        advanceUntilIdle()

        assertEquals(rpgChip, viewModel.activeGenre())
        assertEquals(contentBefore, viewModel.uiState.value.contentState)
        assertEquals(sortingBefore, viewModel.uiState.value.sortBottomSheetState.sorting)
        coVerify(exactly = 0) { searchGamesUseCase(any()) }
    }

    @Test
    fun `Relevance reads Top picks during a browse and Relevance again after clear or a submitted query`() =
        runTest(testDispatcher) {
            givenBrowse(12, testGame(id = 1))
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 2)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()
            val topPicks = UiText.StringResource(Res.string.sort_top_picks)
            val relevance = UiText.StringResource(CoreUiRes.string.sort_relevance)
            assertEquals(relevance, viewModel.sortOption(SearchSort.RELEVANCE).label)

            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()
            assertEquals(topPicks, viewModel.sortOption(SearchSort.RELEVANCE).label)

            viewModel.onEvent(SearchUiEvent.OnClearSearch)
            assertEquals(relevance, viewModel.sortOption(SearchSort.RELEVANCE).label)

            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()
            assertEquals(relevance, viewModel.sortOption(SearchSort.RELEVANCE).label)
        }

    @Test
    fun `the selected sort and its direction survive a browse starting and ending`() =
        runTest(testDispatcher) {
            givenBrowse(12, testGame(id = 1))
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RATING)))
            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RATING)))
            val before = viewModel.uiState.value.sortBottomSheetState
            assertTrue(before.isSortActive)
            assertEquals(false, viewModel.sortOption(SearchSort.RATING).descending)

            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()
            val during = viewModel.uiState.value.sortBottomSheetState
            viewModel.onEvent(SearchUiEvent.OnClearSearch)
            val after = viewModel.uiState.value.sortBottomSheetState

            listOf(during, after).forEach { state ->
                assertEquals(before.isSortActive, state.isSortActive)
                assertEquals(
                    before.sorting.map { Triple(it.sortType, it.selected, it.descending) },
                    state.sorting.map { Triple(it.sortType, it.selected, it.descending) }
                )
            }
        }

    @Test
    fun `a browse starts with every filter unselected, even after a filtered text search`() =
        runTest(testDispatcher) {
            val pc = Platform(id = 1, name = "PC")
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(
                    games = listOf(testGame(id = 7, platforms = listOf(pc))),
                    platforms = listOf(pc),
                    genres = listOf(Genre(12, "RPG"))
                )
            )
            coEvery { browseGenreUseCase(12) } returns AppResult.success(
                SearchResult(
                    games = listOf(testGame(id = 1, platforms = listOf(pc), genres = listOf(Genre(12, "RPG")))),
                    platforms = listOf(pc),
                    genres = emptyList()
                )
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()
            viewModel.onEvent(
                SearchUiEvent.OnFilterClick(
                    viewModel.successState().filters.filterIsInstance<GameFilterUiModel.Platform>().first()
                )
            )
            assertTrue(viewModel.successState().filters.any { it.selected })

            viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
            advanceUntilIdle()

            assertTrue(viewModel.successState().filters.none { it.selected })
            assertTrue(viewModel.successState().filters.filterIsInstance<GameFilterUiModel.Genre>().isEmpty())
        }

    @Test
    fun `filters on a browse narrow with AND semantics in Top picks order`() = runTest(testDispatcher) {
        val pc = Platform(id = 1, name = "PC")
        val rpg = Genre(12, "RPG")
        val indie = Genre(32, "Indie")
        coEvery { browseGenreUseCase(12) } returns AppResult.success(
            SearchResult(
                games = listOf(
                    testGame(id = 4, platforms = listOf(pc), genres = listOf(rpg, indie)),
                    testGame(id = 3, genres = listOf(rpg, indie)),
                    testGame(id = 2, platforms = listOf(pc), genres = listOf(rpg)),
                    testGame(id = 1, platforms = listOf(pc), genres = listOf(rpg, indie))
                ),
                platforms = listOf(pc),
                genres = listOf(indie)
            )
        )
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()

        viewModel.successState().filters
            .filter { it is GameFilterUiModel.Platform || it is GameFilterUiModel.Genre }
            .forEach { viewModel.onEvent(SearchUiEvent.OnFilterClick(it)) }

        assertEquals(listOf(4, 1), viewModel.successState().games.map { it.id })
    }

    @Test
    fun `a genre browse never writes to the search history, retried or not`() = runTest(testDispatcher) {
        coEvery { browseGenreUseCase(12) } returnsMany listOf(
            AppResult.failure(RepositoryError.NoNetwork),
            AppResult.success(
                SearchResult(games = listOf(testGame(id = 1)), platforms = emptyList(), genres = emptyList())
            )
        )
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.OnRetrySearch)
        advanceUntilIdle()

        coVerify(exactly = 0) { addSearchToHistoryUseCase(any()) }
    }

    @Test
    fun `a query submitted after a browse is the only thing written to history`() = runTest(testDispatcher) {
        givenBrowse(12, testGame(id = 1))
        coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
            SearchResult(games = listOf(testGame(id = 2)), platforms = emptyList(), genres = emptyList())
        )
        val viewModel = createViewModel()

        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
        advanceUntilIdle()

        coVerify(exactly = 1) { addSearchToHistoryUseCase(any()) }
        coVerify(exactly = 1) { addSearchToHistoryUseCase("zelda") }
    }

    // endregion

    // region Shelf grid

    private fun givenFeed(feed: DiscoverFeed) {
        every { getDiscoverFeedUseCase(any()) } returns flowOf(AppResult.success(feed))
    }

    private fun gamesFrom(firstId: Int, count: Int): List<Game> =
        (firstId until firstId + count).map { testGame(id = it) }

    private fun genreShelfOf(genreId: Int, name: String, games: List<Game>) =
        RecommendedShelf(ShelfReason.ByGenre(Genre(genreId, name)), games)

    /** The shelf as the screen holds it, which is what the arrow sends back. */
    private fun SearchViewModel.shelf(key: DiscoverShelfKey): DiscoverShelfUiModel {
        val content = uiState.value.discover as DiscoverContentState.Content
        return (listOfNotNull(content.popular, content.upcoming) + content.recommended).first { it.key == key }
    }

    private val popularKey = DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH)
    private val anticipatedKey = DiscoverShelfKey.Lane(DiscoverLane.MOST_ANTICIPATED)

    private fun SearchViewModel.openShelf(key: DiscoverShelfKey) {
        onEvent(SearchUiEvent.OnShelfSeeAllTriggered(shelf(key)))
    }

    @Test
    fun `opening a shelf shows every game of it at once, with no Loading in between`() =
        runTest(testDispatcher) {
            val popular = gamesFrom(firstId = 1, count = 8)
            givenFeed(DiscoverFeed(popular = popular, upcoming = emptyList()))
            val viewModel = createViewModel()
            val shelf = viewModel.shelf(popularKey)
            assertTrue(shelf.hasMore)
            assertEquals(6, shelf.games.size)

            val seen = mutableListOf<SearchContentState>()
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { seen += it.contentState }
            }

            viewModel.onEvent(SearchUiEvent.OnShelfSeeAllTriggered(shelf))

            // Synchronous: nothing had to run on a dispatcher for the grid to be there.
            val success = viewModel.successState()
            assertEquals(popular.map { it.id }, success.allGames.map { it.id })
            assertEquals(popular.map { it.id }, success.games.map { it.id })
            assertEquals(SearchBrowseUiModel.Shelf(popularKey, shelf.title), viewModel.uiState.value.activeBrowse)
            advanceUntilIdle()
            collector.cancel()
            assertTrue(seen.none { it is SearchContentState.Loading })
            assertTrue(seen.none { it is SearchContentState.Error })
        }

    @Test
    fun `the Most anticipated grid holds the shelf without the hero`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = emptyList(), upcoming = gamesFrom(firstId = 1, count = 9)))
        val viewModel = createViewModel()
        assertEquals(1, (viewModel.uiState.value.discover as DiscoverContentState.Content).hero?.id)

        viewModel.openShelf(anticipatedKey)

        assertEquals((2..9).toList(), viewModel.successState().allGames.map { it.id })
    }

    @Test
    fun `opening a shelf fetches nothing and writes nothing to history`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        val viewModel = createViewModel()

        viewModel.openShelf(popularKey)
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.OnClearSearch)

        verify(exactly = 1) { getDiscoverFeedUseCase(any()) }
        coVerify(exactly = 0) { searchGamesUseCase(any()) }
        coVerify(exactly = 0) { browseGenreUseCase(any()) }
        coVerify(exactly = 0) { addSearchToHistoryUseCase(any()) }
    }

    @Test
    fun `opening a shelf empties the text field and the suggestions`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        val viewModel = createViewModel()
        viewModel.setQuery("zel")

        viewModel.openShelf(popularKey)
        advanceUntilIdle()

        assertEquals("", viewModel.textFieldState.text.toString())
        assertEquals(emptyList<String>(), viewModel.uiState.value.suggestions.historySuggestions)
    }

    @Test
    fun `an open shelf grid does not change when the feed refreshes underneath it`() =
        runTest(testDispatcher) {
            val feed = MutableStateFlow(
                AppResult.success(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
            )
            every { getDiscoverFeedUseCase(any()) } returns feed
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)

            feed.value = AppResult.success(
                DiscoverFeed(popular = gamesFrom(firstId = 50, count = 8), upcoming = emptyList())
            )
            advanceUntilIdle()

            assertEquals((1..8).toList(), viewModel.successState().allGames.map { it.id })
            val content = viewModel.uiState.value.discover as DiscoverContentState.Content
            assertEquals((50..55).toList(), content.popular?.games?.map { it.id })
        }

    @Test
    fun `saving and long-pressing a card in a shelf grid act on that game`() = runTest(testDispatcher) {
        val popular = gamesFrom(firstId = 1, count = 8)
        givenFeed(DiscoverFeed(popular = popular, upcoming = emptyList()))
        every { getWishlistAssignmentsUseCase(8) } returns flowOf(
            listOf(WishlistAssignment(WishlistList(id = 10, name = "Wishlist"), isAssigned = false, isDefault = false))
        )
        val viewModel = createViewModel()
        viewModel.openShelf(popularKey)

        // 8 is past the end of the row, so only the grid can have shown it.
        viewModel.onEvent(SearchUiEvent.OnToggleSave(8))
        viewModel.onEvent(SearchUiEvent.OnOpenListSelector(8))
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleWishlistUseCase(popular.last()) }
        assertEquals(8, viewModel.uiState.value.listSelectorState?.gameId)
    }

    @Test
    fun `confirming a list chosen on a shelf grid card adds that catalogue game to the list`() =
        runTest(testDispatcher) {
            val popular = gamesFrom(firstId = 1, count = 8)
            givenFeed(DiscoverFeed(popular = popular, upcoming = emptyList()))
            val list = WishlistList(id = 10, name = "Backlog")
            every { getWishlistAssignmentsUseCase(8) } returns flowOf(
                listOf(WishlistAssignment(list, isAssigned = false, isDefault = false))
            )
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)

            viewModel.onEvent(SearchUiEvent.OnOpenListSelector(8))
            advanceUntilIdle()
            viewModel.onEvent(SearchUiEvent.OnToggleListSelection(list.id))
            viewModel.onEvent(SearchUiEvent.OnConfirmListSelection)
            advanceUntilIdle()

            // The whole game goes down, not its id: it has no stored row yet for the list to show.
            coVerify(exactly = 1) { addGameToListUseCase(popular.last(), list.id) }
            assertEquals(null, viewModel.uiState.value.listSelectorState)
        }

    @Test
    fun `a wishlist change marks the cards of an open shelf grid saved`() = runTest(testDispatcher) {
        val ids = MutableStateFlow<Set<Int>>(emptySet())
        every { getWishlistedGameIdsUseCase() } returns ids
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        val viewModel = createViewModel()
        viewModel.openShelf(popularKey)
        assertTrue(viewModel.successState().games.none { it.isSaved })

        ids.value = setOf(3, 8)
        advanceUntilIdle()

        assertEquals(listOf(3, 8), viewModel.successState().games.filter { it.isSaved }.map { it.id })
    }

    @Test
    fun `two shelves with the same visible title each open their own games`() = runTest(testDispatcher) {
        givenFeed(
            DiscoverFeed(
                popular = emptyList(),
                upcoming = emptyList(),
                recommended = listOf(
                    genreShelfOf(genreId = 12, name = "Same name", games = gamesFrom(firstId = 1, count = 8)),
                    genreShelfOf(genreId = 31, name = "Same name", games = gamesFrom(firstId = 101, count = 8))
                )
            )
        )
        val viewModel = createViewModel()
        val first = DiscoverShelfKey.ByGenre(12)
        val second = DiscoverShelfKey.ByGenre(31)
        assertEquals(viewModel.shelf(first).title, viewModel.shelf(second).title)

        viewModel.openShelf(second)
        assertEquals((101..108).toList(), viewModel.successState().allGames.map { it.id })
        viewModel.onEvent(SearchUiEvent.OnClearSearch)

        viewModel.openShelf(first)
        assertEquals((1..8).toList(), viewModel.successState().allGames.map { it.id })
    }

    @Test
    fun `tapping a shelf the refreshed feed no longer holds does nothing`() = runTest(testDispatcher) {
        val withShelf = DiscoverFeed(
            popular = gamesFrom(firstId = 1, count = 8),
            upcoming = emptyList(),
            recommended = listOf(genreShelfOf(genreId = 12, name = "RPG", games = gamesFrom(firstId = 101, count = 8)))
        )
        val feed = MutableStateFlow(AppResult.success(withShelf))
        every { getDiscoverFeedUseCase(any()) } returns feed
        val viewModel = createViewModel()
        val staleShelf = viewModel.shelf(DiscoverShelfKey.ByGenre(12))

        feed.value = AppResult.success(withShelf.copy(recommended = emptyList()))
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.OnShelfSeeAllTriggered(staleShelf))
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.activeBrowse)
        assertEquals(SearchContentState.Idle, viewModel.uiState.value.contentState)
    }

    @Test
    fun `a double tap keeps the first snapshot`() = runTest(testDispatcher) {
        val feed = MutableStateFlow(
            AppResult.success(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        )
        every { getDiscoverFeedUseCase(any()) } returns feed
        val viewModel = createViewModel()
        val shelf = viewModel.shelf(popularKey)

        viewModel.onEvent(SearchUiEvent.OnShelfSeeAllTriggered(shelf))
        feed.value = AppResult.success(
            DiscoverFeed(popular = gamesFrom(firstId = 50, count = 8), upcoming = emptyList())
        )
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.OnShelfSeeAllTriggered(shelf))
        advanceUntilIdle()

        assertEquals((1..8).toList(), viewModel.successState().allGames.map { it.id })
    }

    @Test
    fun `a search still loading when a shelf opens cannot land on top of it`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        val deferred = CompletableDeferred<AppResult<SearchResult>>()
        coEvery { searchGamesUseCase("zelda") } coAnswers { deferred.await() }
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
        advanceUntilIdle()
        assertEquals(SearchContentState.Loading, viewModel.uiState.value.contentState)

        viewModel.openShelf(popularKey)
        deferred.complete(
            AppResult.success(SearchResult(games = listOf(testGame(id = 999)), platforms = emptyList(), genres = emptyList()))
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.activeBrowse is SearchBrowseUiModel.Shelf)
        assertEquals((1..8).toList(), viewModel.successState().allGames.map { it.id })
        assertEquals("", viewModel.textFieldState.text.toString())
    }

    @Test
    fun `a search failing while a shelf opens cannot put an Error under it`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        val deferred = CompletableDeferred<AppResult<SearchResult>>()
        coEvery { searchGamesUseCase("zelda") } coAnswers { deferred.await() }
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
        advanceUntilIdle()

        viewModel.openShelf(popularKey)
        deferred.complete(AppResult.failure(RepositoryError.NoNetwork))
        advanceUntilIdle()
        viewModel.onEvent(SearchUiEvent.OnRetrySearch)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contentState is SearchContentState.Success)
        // Retry on a shelf grid is a no-op: it must not re-run the old text search either.
        coVerify(exactly = 1) { searchGamesUseCase("zelda") }
    }

    // endregion

    // region Shelf grid: leaving it, sorting it, filtering it

    private fun givenPopular(vararg games: Game) {
        givenFeed(DiscoverFeed(popular = games.toList(), upcoming = emptyList()))
    }

    private fun gamesWithOrder(pc: Platform) = listOf(
        // Deliberately neither alphabetical nor by rating: only the shelf's own order explains [3, 1, 2, 4].
        testGame(id = 3, name = "Charlie", rating = 10.0, platforms = listOf(pc)),
        testGame(id = 1, name = "Alpha", rating = 90.0, platforms = listOf(pc)),
        testGame(id = 2, name = "Bravo", rating = 50.0),
        testGame(id = 4, name = "Delta", rating = 70.0, platforms = listOf(pc))
    )

    @Test
    fun `OnClearSearch from a shelf grid returns to the feed without fetching it again`() =
        runTest(testDispatcher) {
            givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)

            viewModel.onEvent(SearchUiEvent.OnClearSearch)

            assertEquals(null, viewModel.uiState.value.activeBrowse)
            assertEquals(SearchContentState.Idle, viewModel.uiState.value.contentState)
            verify(exactly = 1) { getDiscoverFeedUseCase(any()) }
        }

    @Test
    fun `a submitted query replaces a shelf grid with text results`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
            SearchResult(games = listOf(testGame(id = 900)), platforms = emptyList(), genres = emptyList())
        )
        val viewModel = createViewModel()
        viewModel.openShelf(popularKey)

        viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.activeBrowse)
        assertEquals(listOf(900), viewModel.successState().allGames.map { it.id })
    }

    @Test
    fun `a genre tapped while a shelf grid is open replaces it`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        givenBrowse(12, testGame(id = 700))
        val viewModel = createViewModel()
        viewModel.openShelf(popularKey)

        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()

        assertEquals(rpgChip, viewModel.activeGenre())
        assertEquals(listOf(700), viewModel.successState().allGames.map { it.id })
    }

    @Test
    fun `a shelf opened while a genre browse is showing replaces it`() = runTest(testDispatcher) {
        givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
        givenBrowse(12, testGame(id = 700))
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnGenreBrowseTriggered(rpgChip))
        advanceUntilIdle()

        viewModel.openShelf(popularKey)

        assertEquals(null, viewModel.activeGenre())
        assertTrue(viewModel.uiState.value.activeBrowse is SearchBrowseUiModel.Shelf)
        assertEquals((1..8).toList(), viewModel.successState().allGames.map { it.id })
    }

    @Test
    fun `typing during a shelf grid leaves it on screen, even if the text is deleted again`() =
        runTest(testDispatcher) {
            givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)
            val open = viewModel.uiState.value.activeBrowse

            viewModel.setQuery("ze")
            advanceUntilIdle()
            viewModel.setQuery("")
            advanceUntilIdle()

            assertEquals(open, viewModel.uiState.value.activeBrowse)
            assertEquals((1..8).toList(), viewModel.successState().allGames.map { it.id })
        }

    @Test
    fun `the search bar shows the shelf's own title while its grid is open`() = runTest(testDispatcher) {
        givenFeed(
            DiscoverFeed(
                popular = gamesFrom(firstId = 1, count = 8),
                upcoming = emptyList(),
                recommended = listOf(genreShelfOf(genreId = 12, name = "RPG", games = gamesFrom(firstId = 101, count = 8)))
            )
        )
        val viewModel = createViewModel()

        listOf(popularKey, DiscoverShelfKey.ByGenre(12)).forEach { key ->
            val shelf = viewModel.shelf(key)
            viewModel.openShelf(key)

            assertEquals(shelf.title, viewModel.uiState.value.activeBrowse!!.toPlaceholder())
            viewModel.onEvent(SearchUiEvent.OnClearSearch)
        }
    }

    @Test
    fun `Relevance reads Top picks while a shelf grid is open and Relevance again after it`() =
        runTest(testDispatcher) {
            givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 900)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()
            val topPicks = UiText.StringResource(Res.string.sort_top_picks)
            val relevance = UiText.StringResource(CoreUiRes.string.sort_relevance)
            assertEquals(relevance, viewModel.sortOption(SearchSort.RELEVANCE).label)

            viewModel.openShelf(popularKey)
            assertEquals(topPicks, viewModel.sortOption(SearchSort.RELEVANCE).label)

            viewModel.onEvent(SearchUiEvent.OnClearSearch)
            assertEquals(relevance, viewModel.sortOption(SearchSort.RELEVANCE).label)

            viewModel.openShelf(popularKey)
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()
            assertEquals(relevance, viewModel.sortOption(SearchSort.RELEVANCE).label)
        }

    @Test
    fun `the selected sort and its direction survive a shelf grid opening and closing`() =
        runTest(testDispatcher) {
            givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RATING)))
            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RATING)))
            val before = viewModel.uiState.value.sortBottomSheetState
            assertEquals(false, viewModel.sortOption(SearchSort.RATING).descending)

            viewModel.openShelf(popularKey)
            val during = viewModel.uiState.value.sortBottomSheetState
            viewModel.onEvent(SearchUiEvent.OnClearSearch)
            val after = viewModel.uiState.value.sortBottomSheetState

            listOf(during, after).forEach { state ->
                assertEquals(before.isSortActive, state.isSortActive)
                assertEquals(
                    before.sorting.map { Triple(it.sortType, it.selected, it.descending) },
                    state.sorting.map { Triple(it.sortType, it.selected, it.descending) }
                )
            }
        }

    @Test
    fun `a shelf grid opens in the order of a sort already chosen`() = runTest(testDispatcher) {
        val pc = Platform(id = 1, name = "PC")
        givenPopular(*gamesWithOrder(pc).toTypedArray())
        val viewModel = createViewModel()
        viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.NAME)))

        viewModel.openShelf(popularKey)

        assertEquals(listOf("Delta", "Charlie", "Bravo", "Alpha"), viewModel.successState().games.map { it.name })
        // allGames stays in the shelf's own order, so Top picks can be recovered.
        assertEquals(listOf(3, 1, 2, 4), viewModel.successState().allGames.map { it.id })
    }

    @Test
    fun `Top picks keeps the shelf's order and ascending reverses it, even after a filter`() =
        runTest(testDispatcher) {
            val pc = Platform(id = 1, name = "PC")
            givenPopular(*gamesWithOrder(pc).toTypedArray())
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)

            assertEquals(listOf(3, 1, 2, 4), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RELEVANCE)))
            assertEquals(listOf(4, 2, 1, 3), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RELEVANCE)))
            val platformFilter =
                viewModel.successState().filters.filterIsInstance<GameFilterUiModel.Platform>().first()
            viewModel.onEvent(SearchUiEvent.OnFilterClick(platformFilter))
            assertEquals(listOf(3, 1, 4), viewModel.successState().games.map { it.id })
        }

    @Test
    fun `Name, Rating and Release date sort a shelf grid as they sort a text search`() =
        runTest(testDispatcher) {
            givenPopular(
                testGame(id = 3, name = "Charlie", rating = 10.0, ratingCount = 5).copy(releaseDate = "2024-01-01"),
                testGame(id = 1, name = "Alpha", rating = 90.0, ratingCount = 5).copy(releaseDate = "2025-06-01"),
                testGame(id = 2, name = "Bravo", rating = 50.0, ratingCount = 5).copy(releaseDate = "2023-03-01")
            )
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.NAME)))
            assertEquals(listOf(3, 2, 1), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RATING)))
            assertEquals(listOf(1, 2, 3), viewModel.successState().games.map { it.id })

            viewModel.onEvent(SearchUiEvent.OnSortChanged(viewModel.sortOption(SearchSort.RELEASE_DATE)))
            assertEquals(listOf(1, 3, 2), viewModel.successState().games.map { it.id })
        }

    @Test
    fun `a shelf grid offers every genre its games carry, with none withheld`() = runTest(testDispatcher) {
        val rpg = Genre(12, "RPG")
        val adventure = Genre(31, "Adventure")
        val shooter = Genre(5, "Shooter")
        val pc = Platform(id = 1, name = "PC")
        givenPopular(
            testGame(id = 1, platforms = listOf(pc), genres = listOf(rpg, adventure)),
            testGame(id = 2, genres = listOf(rpg, shooter)),
            testGame(id = 3, genres = listOf(rpg))
        )
        val viewModel = createViewModel()

        viewModel.openShelf(popularKey)

        val filters = viewModel.successState().filters
        // Ordered by how many of the games carry each, as for a text search: RPG (3), then the two singles.
        assertEquals(listOf(12, 31, 5), filters.filterIsInstance<GameFilterUiModel.Genre>().map { it.id })
        assertEquals(listOf(1), filters.filterIsInstance<GameFilterUiModel.Platform>().map { it.id })
        assertTrue(filters.filterIsInstance<GameFilterUiModel.GameType>().isNotEmpty())
        coVerify(exactly = 0) { browseGenreUseCase(any()) }
    }

    @Test
    fun `a shelf grid starts with every filter unselected, even after a filtered text search`() =
        runTest(testDispatcher) {
            val pc = Platform(id = 1, name = "PC")
            givenPopular(testGame(id = 1, platforms = listOf(pc)), testGame(id = 2))
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(
                    games = listOf(testGame(id = 7, platforms = listOf(pc))),
                    platforms = listOf(pc),
                    genres = emptyList()
                )
            )
            val viewModel = createViewModel()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()
            viewModel.onEvent(
                SearchUiEvent.OnFilterClick(
                    viewModel.successState().filters.filterIsInstance<GameFilterUiModel.Platform>().first()
                )
            )
            assertTrue(viewModel.successState().filters.any { it.selected })

            viewModel.openShelf(popularKey)

            assertTrue(viewModel.successState().filters.none { it.selected })
            assertEquals(listOf(1, 2), viewModel.successState().games.map { it.id })
        }

    @Test
    fun `filters on a shelf grid narrow with AND semantics in the shelf's order`() = runTest(testDispatcher) {
        val pc = Platform(id = 1, name = "PC")
        val rpg = Genre(12, "RPG")
        val indie = Genre(32, "Indie")
        givenPopular(
            testGame(id = 4, platforms = listOf(pc), genres = listOf(rpg, indie)),
            testGame(id = 3, genres = listOf(rpg, indie)),
            testGame(id = 2, platforms = listOf(pc), genres = listOf(rpg)),
            testGame(id = 1, platforms = listOf(pc), genres = listOf(rpg, indie))
        )
        val viewModel = createViewModel()
        viewModel.openShelf(popularKey)

        viewModel.successState().filters
            .filter { it is GameFilterUiModel.Platform || it is GameFilterUiModel.Genre }
            .forEach { viewModel.onEvent(SearchUiEvent.OnFilterClick(it)) }

        assertEquals(listOf(4, 1), viewModel.successState().games.map { it.id })
    }

    @Test
    fun `filters that match nothing leave a Success with no games, and clearing them restores the shelf`() =
        runTest(testDispatcher) {
            val pc = Platform(id = 1, name = "PC")
            val indie = Genre(32, "Indie")
            givenPopular(
                testGame(id = 1, platforms = listOf(pc)),
                testGame(id = 2, genres = listOf(indie))
            )
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)

            viewModel.successState().filters
                .filter { it is GameFilterUiModel.Platform || it is GameFilterUiModel.Genre }
                .forEach { viewModel.onEvent(SearchUiEvent.OnFilterClick(it)) }

            // Success, not Empty: Empty would drop the filter chips and the way to clear them.
            assertEquals(emptyList<Int>(), viewModel.successState().games.map { it.id })
            assertTrue(viewModel.uiState.value.activeBrowse is SearchBrowseUiModel.Shelf)

            viewModel.onEvent(SearchUiEvent.OnClearActiveFilters)

            assertEquals(listOf(1, 2), viewModel.successState().games.map { it.id })
        }

    @Test
    fun `the filter sheet opens on a shelf grid with the shelf's filters and match count`() =
        runTest(testDispatcher) {
            val pc = Platform(id = 1, name = "PC")
            givenPopular(testGame(id = 1, platforms = listOf(pc)), testGame(id = 2))
            val viewModel = createViewModel()
            viewModel.openShelf(popularKey)

            viewModel.onEvent(SearchUiEvent.OnOpenFilters)

            val sheet = viewModel.uiState.value.filtersBottomSheetState
            assertTrue(sheet.isVisible)
            assertEquals(2, sheet.matchCount)
            assertEquals(viewModel.successState().filters, sheet.filters)
        }

    @Test
    fun `opening shelf grids never writes to the search history`() = runTest(testDispatcher) {
        givenFeed(
            DiscoverFeed(
                popular = gamesFrom(firstId = 1, count = 8),
                upcoming = gamesFrom(firstId = 101, count = 9),
                recommended = listOf(genreShelfOf(genreId = 12, name = "RPG", games = gamesFrom(firstId = 201, count = 8)))
            )
        )
        val viewModel = createViewModel()

        listOf(popularKey, anticipatedKey, DiscoverShelfKey.ByGenre(12)).forEach { key ->
            viewModel.openShelf(key)
            advanceUntilIdle()
            viewModel.onEvent(SearchUiEvent.OnClearSearch)
        }

        coVerify(exactly = 0) { addSearchToHistoryUseCase(any()) }
    }

    @Test
    fun `a query submitted after a shelf grid is the only thing written to history`() =
        runTest(testDispatcher) {
            givenFeed(DiscoverFeed(popular = gamesFrom(firstId = 1, count = 8), upcoming = emptyList()))
            coEvery { searchGamesUseCase("zelda") } returns AppResult.success(
                SearchResult(games = listOf(testGame(id = 900)), platforms = emptyList(), genres = emptyList())
            )
            val viewModel = createViewModel()

            viewModel.openShelf(popularKey)
            advanceUntilIdle()
            viewModel.onEvent(SearchUiEvent.OnSearchTriggered("zelda"))
            advanceUntilIdle()

            coVerify(exactly = 1) { addSearchToHistoryUseCase(any()) }
            coVerify(exactly = 1) { addSearchToHistoryUseCase("zelda") }
        }

    // endregion
}

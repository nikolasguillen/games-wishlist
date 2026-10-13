package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.ui.component.ErrorPage
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.DiscoverContentState
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfKey
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.model.GameFilterUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchBrowseUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchContentState
import com.nikolasguillen.questlog.feature.search.model.SearchUiEvent
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_most_anticipated
import com.nikolasguillen.questlog.feature.search.resources.discover_popular_this_month
import com.nikolasguillen.questlog.feature.search.resources.genre_browse_no_results
import com.nikolasguillen.questlog.feature.search.resources.search_no_results
import org.jetbrains.compose.resources.stringResource

/**
 * The screen's single content area, shared by two independent contents. [contentState] decides which:
 * [SearchContentState.Idle] means no search is running, so the area belongs to [discoverState].
 *
 * [activeBrowse] is what the grid is showing when it is not a typed query, if anything. A browse renders like
 * a text search, so it only decides which message the empty state shows.
 */
@Composable
internal fun SearchMainContent(
    contentState: SearchContentState,
    discoverState: DiscoverContentState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    gridState: LazyGridState,
    activeBrowse: SearchBrowseUiModel?,
    modifier: Modifier = Modifier,
    discoverListState: LazyListState = rememberLazyListState()
) {
    Box(modifier = modifier) {
        when (contentState) {
            is SearchContentState.Idle -> DiscoverContent(
                state = discoverState,
                onEvent = onEvent,
                onGameClick = onGameClick,
                listState = discoverListState
            )
            is SearchContentState.Error -> ErrorPage(
                message = contentState.message,
                onRetryClick = { onEvent(SearchUiEvent.OnRetrySearch) }
            )
            is SearchContentState.Empty -> EmptySearchPlaceholder(
                message = if (activeBrowse is SearchBrowseUiModel.Genre) {
                    stringResource(Res.string.genre_browse_no_results, activeBrowse.genre.label.asString())
                } else {
                    stringResource(Res.string.search_no_results)
                },
                onClearSearchClick = { onEvent(SearchUiEvent.OnClearSearch) }
            )
            is SearchContentState.Loading -> LoadingPage()
            is SearchContentState.Success -> {
                SearchResultGrid(
                    games = contentState.games,
                    activeFilters = contentState.activeFilters,
                    onFilterClick = { onEvent(SearchUiEvent.OnFilterClick(it)) },
                    onGameClick = onGameClick,
                    onSaveClick = { onEvent(SearchUiEvent.OnToggleSave(it)) },
                    onLongClick = { onEvent(SearchUiEvent.OnOpenListSelector(it)) },
                    onClearFiltersClick = { onEvent(SearchUiEvent.OnClearActiveFilters) },
                    state = gridState
                )
            }
        }
    }
}

@Composable
private fun DiscoverContent(
    state: DiscoverContentState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    listState: LazyListState
) {
    when (state) {
        is DiscoverContentState.Loading -> LoadingPage()
        is DiscoverContentState.Error -> ErrorPage(
            message = state.message,
            onRetryClick = { onEvent(SearchUiEvent.OnRetryDiscover) }
        )
        is DiscoverContentState.Content -> DiscoverFeed(
            hero = state.hero,
            popular = state.popular,
            upcoming = state.upcoming,
            onGameClick = onGameClick,
            onSaveClick = { onEvent(SearchUiEvent.OnToggleSave(it)) },
            onLongClick = { onEvent(SearchUiEvent.OnOpenListSelector(it)) },
            recommended = state.recommended,
            isStale = state.isStale,
            isRefreshing = state.isRefreshing,
            onRefreshClick = { onEvent(SearchUiEvent.OnRefreshDiscover) },
            onDismissRefreshClick = { onEvent(SearchUiEvent.OnDismissDiscoverRefresh) },
            state = listState
        )
    }
}

@Composable
private fun SearchMainContentPreview(
    contentState: SearchContentState,
    discoverState: DiscoverContentState = DiscoverContentState.Loading,
    activeBrowse: SearchBrowseUiModel? = null
) {
    QuestLogTheme {
        SearchMainContent(
            contentState = contentState,
            discoverState = discoverState,
            onEvent = {},
            onGameClick = {},
            gridState = rememberLazyGridState(),
            activeBrowse = activeBrowse
        )
    }
}

@QuestLogPreviews
@Composable
private fun SearchMainContentDiscoverPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Idle,
        discoverState = DiscoverContentState.Content(
            hero = GameItemUiModel.getDummy().copy(id = 3, name = "Hollow Knight: Silksong"),
            popular = DiscoverShelfUiModel(
                key = DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH),
                title = UiText.StringResource(Res.string.discover_popular_this_month),
                games = listOf(GameItemUiModel.getDummy()),
                hasMore = false
            ),
            upcoming = DiscoverShelfUiModel(
                key = DiscoverShelfKey.Lane(DiscoverLane.MOST_ANTICIPATED),
                title = UiText.StringResource(Res.string.discover_most_anticipated),
                games = listOf(GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")),
                hasMore = false
            )
        )
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentDiscoverErrorPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Idle,
        discoverState = DiscoverContentState.Error(
            UiText.DynamicString("Could not load the feed.")
        )
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentLoadingPreview() {
    SearchMainContentPreview(SearchContentState.Loading)
}

@QuestLogPreviews
@Composable
private fun SearchMainContentEmptyPreview() {
    SearchMainContentPreview(SearchContentState.Empty)
}

@QuestLogPreviews
@Composable
private fun SearchMainContentGenreBrowseEmptyPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Empty,
        activeBrowse = SearchBrowseUiModel.Genre(
            BrowseGenreUiModel(id = 30, label = UiText.DynamicString("Pinball"))
        )
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentErrorPreview() {
    SearchMainContentPreview(
        SearchContentState.Error(UiText.DynamicString("No internet connection."))
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentSuccessPreview() {
    SearchMainContentPreview(
        SearchContentState.Success(
            games = listOf(
                GameItemUiModel.getDummy(),
                GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
            ),
            filters = listOf(
                GameFilterUiModel.Platform(id = 0, label = UiText.DynamicString("PC"), selected = true)
            )
        )
    )
}

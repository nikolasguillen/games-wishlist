package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import com.nikolasguillen.questlog.feature.search.model.EntityResultUiModel
import com.nikolasguillen.questlog.feature.search.model.GameFilterUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchBrowseUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchContentState
import com.nikolasguillen.questlog.feature.search.model.SearchType
import com.nikolasguillen.questlog.feature.search.model.SearchUiEvent
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_most_anticipated
import com.nikolasguillen.questlog.feature.search.resources.discover_popular_this_month
import com.nikolasguillen.questlog.feature.search.resources.genre_browse_no_results
import com.nikolasguillen.questlog.feature.search.resources.search_no_characters
import com.nikolasguillen.questlog.feature.search.resources.search_no_developers
import com.nikolasguillen.questlog.feature.search.resources.search_no_results
import org.jetbrains.compose.resources.stringResource

/**
 * The screen's single content area, shared by two independent contents. [contentState] decides which:
 * [SearchContentState.Idle] means no search is running, so the area belongs to [discoverState].
 *
 * [activeBrowse] is what the grid is showing when it is not a typed query, if anything. A browse renders like
 * a text search, so it only decides which message the empty state shows. [resultsType] is what the committed
 * search looked up, which picks the cards of [SearchContentState.EntityResults] and the empty message.
 *
 * The Discover feed is a slot, [discoverFeed], so each screen layout supplies its own: the compact layout draws
 * [DiscoverFeed] and the wide one [WideDiscoverFeed]. This composable keeps the loading and error states of the
 * feed and hands the loaded content to the slot together with the list state it must scroll with.
 */
@Composable
internal fun SearchMainContent(
    contentState: SearchContentState,
    discoverState: DiscoverContentState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onDeveloperClick: (Int) -> Unit,
    gridState: LazyGridState,
    activeBrowse: SearchBrowseUiModel?,
    resultsType: SearchType,
    discoverFeed: @Composable (content: DiscoverContentState.Content, listState: LazyListState) -> Unit,
    modifier: Modifier = Modifier,
    discoverListState: LazyListState = rememberLazyListState()
) {
    Box(modifier = modifier) {
        when (contentState) {
            is SearchContentState.Idle -> DiscoverContent(
                state = discoverState,
                onEvent = onEvent,
                listState = discoverListState,
                discoverFeed = discoverFeed
            )
            is SearchContentState.Error -> ErrorPage(
                message = contentState.message,
                onRetryClick = { onEvent(SearchUiEvent.OnRetrySearch) }
            )
            is SearchContentState.Empty -> EmptySearchPlaceholder(
                message = if (activeBrowse is SearchBrowseUiModel.Genre) {
                    stringResource(Res.string.genre_browse_no_results, activeBrowse.genre.label.asString())
                } else {
                    stringResource(
                        when (resultsType) {
                            SearchType.GAMES -> Res.string.search_no_results
                            SearchType.CHARACTERS -> Res.string.search_no_characters
                            SearchType.DEVELOPERS -> Res.string.search_no_developers
                        }
                    )
                },
                onClearSearchClick = { onEvent(SearchUiEvent.OnClearSearch) }
            )
            is SearchContentState.Loading -> LoadingPage()
            is SearchContentState.EntityResults -> EntityResultGrid(
                items = contentState.items,
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) { item, cardModifier ->
                when (resultsType) {
                    SearchType.CHARACTERS -> CharacterResultCard(
                        item = item,
                        onClick = { onCharacterClick(item.id) },
                        modifier = cardModifier
                    )
                    SearchType.DEVELOPERS -> DeveloperResultCard(
                        item = item,
                        onClick = { onDeveloperClick(item.id) },
                        modifier = cardModifier
                    )
                    // Games never produce entity results.
                    SearchType.GAMES -> Unit
                }
            }
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
    listState: LazyListState,
    discoverFeed: @Composable (content: DiscoverContentState.Content, listState: LazyListState) -> Unit
) {
    when (state) {
        is DiscoverContentState.Loading -> LoadingPage()
        is DiscoverContentState.Error -> ErrorPage(
            message = state.message,
            onRetryClick = { onEvent(SearchUiEvent.OnRetryDiscover) }
        )
        is DiscoverContentState.Content -> discoverFeed(state, listState)
    }
}

@Composable
private fun SearchMainContentPreview(
    contentState: SearchContentState,
    discoverState: DiscoverContentState = DiscoverContentState.Loading,
    activeBrowse: SearchBrowseUiModel? = null,
    resultsType: SearchType = SearchType.GAMES,
    discoverFeed: @Composable (DiscoverContentState.Content, LazyListState) -> Unit = { content, listState ->
        PreviewDiscoverFeed(content, listState)
    }
) {
    QuestLogTheme {
        SearchMainContent(
            contentState = contentState,
            discoverState = discoverState,
            onEvent = {},
            onGameClick = {},
            onCharacterClick = {},
            onDeveloperClick = {},
            gridState = rememberLazyGridState(),
            activeBrowse = activeBrowse,
            resultsType = resultsType,
            discoverFeed = discoverFeed
        )
    }
}

private val previewDiscoverContent = DiscoverContentState.Content(
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

@Composable
private fun PreviewDiscoverFeed(content: DiscoverContentState.Content, listState: LazyListState) {
    DiscoverFeed(
        hero = content.hero,
        popular = content.popular,
        upcoming = content.upcoming,
        onGameClick = {},
        onSaveClick = {},
        onLongClick = {},
        onSeeAllClick = {},
        recommended = content.recommended,
        isStale = content.isStale,
        isRefreshing = content.isRefreshing,
        onRefreshClick = {},
        onDismissRefreshClick = {},
        state = listState
    )
}

@Composable
private fun PreviewWideDiscoverFeed(content: DiscoverContentState.Content, listState: LazyListState) {
    WideDiscoverFeed(
        hero = content.hero,
        popular = content.popular,
        upcoming = content.upcoming,
        onGameClick = {},
        onSaveClick = {},
        onLongClick = {},
        onSeeAllClick = {},
        recommended = content.recommended,
        isStale = content.isStale,
        isRefreshing = content.isRefreshing,
        onRefreshClick = {},
        onDismissRefreshClick = {},
        state = listState
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentDiscoverPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Idle,
        discoverState = previewDiscoverContent
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun SearchMainContentWideDiscoverPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Idle,
        discoverState = previewDiscoverContent,
        discoverFeed = { content, listState -> PreviewWideDiscoverFeed(content, listState) }
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

@QuestLogPreviews
@Composable
private fun SearchMainContentCharactersPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.EntityResults(
            items = listOf(
                EntityResultUiModel(id = 1, name = "Kratos", imageUrl = null),
                EntityResultUiModel(id = 2, name = "Young Kratos", imageUrl = null)
            )
        ),
        resultsType = SearchType.CHARACTERS
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentCharactersEmptyPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Empty,
        resultsType = SearchType.CHARACTERS
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentDevelopersPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.EntityResults(
            items = listOf(
                EntityResultUiModel(id = 1, name = "Capcom", imageUrl = null),
                EntityResultUiModel(id = 2, name = "Capcom Vancouver", imageUrl = null)
            )
        ),
        resultsType = SearchType.DEVELOPERS
    )
}

@QuestLogPreviews
@Composable
private fun SearchMainContentDevelopersEmptyPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Empty,
        resultsType = SearchType.DEVELOPERS
    )
}

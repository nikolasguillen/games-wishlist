package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.component.MainScreenHeader
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.core.ui.resources.proceed_label
import com.nikolasguillen.questlog.core.ui.util.annotatedStringResource
import com.nikolasguillen.questlog.feature.search.mapper.defaultBrowseGenres
import com.nikolasguillen.questlog.feature.search.mapper.toPlaceholder
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.GameFilterUiModel
import com.nikolasguillen.questlog.feature.search.model.GameSuggestionUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchContentState
import com.nikolasguillen.questlog.feature.search.model.SearchHistoryUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchSuggestionsUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchUiEvent
import com.nikolasguillen.questlog.feature.search.model.SearchUiState
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.back_to_discover_content_description
import com.nikolasguillen.questlog.feature.search.resources.clear_history_message
import com.nikolasguillen.questlog.feature.search.resources.clear_history_title
import com.nikolasguillen.questlog.feature.search.resources.clear_search_content_description
import com.nikolasguillen.questlog.feature.search.resources.remove_history_item
import com.nikolasguillen.questlog.feature.search.resources.remove_history_item_message
import com.nikolasguillen.questlog.feature.search.resources.search_content_description
import com.nikolasguillen.questlog.feature.search.resources.search_placeholder
import com.nikolasguillen.questlog.feature.search.resources.suggestions_section_games
import com.nikolasguillen.questlog.feature.search.resources.suggestions_section_recent
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/** Rows of shimmer while the debounced fetch runs. The remote call caps the real rows at four. */
private const val LOADING_SUGGESTION_COUNT = 3

/**
 * Corner radius that turns a [SearchBarDefaults.InputFieldHeight]-tall shape into a full pill (the
 * search bar) or a perfect circle (the back-to-discover button) — half of that fixed height.
 */
@OptIn(ExperimentalMaterial3Api::class)
private val FullyRoundedCornerRadius = SearchBarDefaults.InputFieldHeight / 2

/**
 * The flat corner radius on the side where the back-to-discover button and the search bar meet, once
 * both are visible. The single source for [SearchTopBar]'s animation target and [CollapsedSearchBar]'s
 * static button shape — the two would otherwise have to independently agree on the same literal for the
 * shapes to still read as cut from the same pill once the button settles in.
 */
private val AdjoiningFlatCornerRadius: Dp
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.spacing.small

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTopBar(
    uiState: SearchUiState,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    scrollBehavior: SearchBarScrollBehavior,
    onSearch: (String) -> Unit,
    onGameClick: (Int) -> Unit,
    onBrowseGenre: (BrowseGenreUiModel) -> Unit,
    onEvent: (SearchUiEvent) -> Unit,
    onProfileClick: () -> Unit,
    backgroundColor: Color
) {
    val searchBarColors = SearchBarDefaults.containedColors(state = searchBarState).copy(
        dividerColor = SearchBarDefaults.colors().dividerColor.copy(alpha = 0.5f)
    )

    // Idle is the only state where the Discover feed already owns the content area: there is nothing
    // to go back to, so the arrow only exists while committed results hold the screen.
    val isShowingResults = uiState.contentState !is SearchContentState.Idle

    // The corner radius of the side where the back button and the search bar meet: fully rounded when
    // the button is hidden (a separate pill and circle), animated down to flat when both are visible, so
    // the two read as cut from the same pill — still kept visually apart by the gap in the button's own
    // slot. Computed once here, alongside [isShowingResults], because [inputField] below is the same
    // instance Material3 carries through the collapsed-to-expanded transition, so the input field's own
    // shape has to come from the same source as the collapsed pill's.
    val adjoiningCornerRadius by animateDpAsState(
        targetValue = if (isShowingResults) AdjoiningFlatCornerRadius else FullyRoundedCornerRadius,
        label = "adjoiningCornerRadius"
    )
    val searchBarShape = RoundedCornerShape(
        topStart = adjoiningCornerRadius,
        bottomStart = adjoiningCornerRadius,
        topEnd = FullyRoundedCornerRadius,
        bottomEnd = FullyRoundedCornerRadius
    )

    val inputField = @Composable {
        SearchInputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            shape = searchBarShape,
            onSearch = { onSearch(textFieldState.text.toString()) },
            onClearSearch = { onEvent(SearchUiEvent.OnClearSearch) },
            browsePlaceholder = uiState.activeBrowse?.toPlaceholder()
        )
    }

    CollapsedSearchBar(
        searchBarState = searchBarState,
        searchBarColors = searchBarColors,
        inputField = inputField,
        onProfileClick = onProfileClick,
        backgroundColor = backgroundColor,
        showBackToDiscover = isShowingResults,
        onBackToDiscover = { onEvent(SearchUiEvent.OnClearSearch) },
        searchBarShape = searchBarShape,
        modifier = scrollBehavior.barModifier,
        bottomContent = {
            val state = uiState.contentState
            if (state is SearchContentState.Success) {
                SearchSubHeader(
                    resultsCount = state.games.size,
                    isSortActive = uiState.sortBottomSheetState.isSortActive,
                    onOpenSort = { onEvent(SearchUiEvent.OnOpenSort) },
                    onOpenFilters = { onEvent(SearchUiEvent.OnOpenFilters) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }
        }
    )

    ExpandedSearchBar(
        searchBarState = searchBarState,
        inputField = inputField,
        history = uiState.history,
        suggestions = uiState.suggestions,
        browseGenres = uiState.browseGenres,
        searchQuery = textFieldState.text.toString(),
        searchBarColors = searchBarColors,
        onCommitSearch = onSearch,
        onBrowseGenre = onBrowseGenre,
        onGameClick = onGameClick,
        onRemoveRecentGame = { onEvent(SearchUiEvent.OnRecentGameRemoved(it)) },
        onClearRecentSearches = { onEvent(SearchUiEvent.OnClearHistory) },
        onRemoveRecentSearchItem = { onEvent(SearchUiEvent.OnHistoryItemRemoved(it)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    shape: Shape = SearchBarDefaults.inputFieldShape,
    browsePlaceholder: UiText? = null
) {
    val searchInputFieldColor = if (searchBarState.currentValue == SearchBarValue.Collapsed) {
        MaterialTheme.appColors.searchBarInputFieldColor
    } else {
        MaterialTheme.appColors.expandedSearchBarColor
    }

    // A browse (a genre or a Discover shelf) leaves the field empty, so the placeholder is what says what is
    // on screen. A screen reader skips the default placeholder (hidden from semantics below), so the browse is
    // also announced as the field's state, or an empty field would be all a TalkBack user hears.
    val browseText = browsePlaceholder?.asString()

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        modifier = if (browseText != null) {
            Modifier.semantics { stateDescription = browseText }
        } else {
            Modifier
        },
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = searchInputFieldColor,
            unfocusedContainerColor = searchInputFieldColor
        ),
        onSearch = { onSearch() },
        readOnly = searchBarState.currentValue == SearchBarValue.Collapsed,
        placeholder = {
            Text(
                text = browseText ?: stringResource(Res.string.search_placeholder),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics {}
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Always goes through onClearSearch, expanded or collapsed: an emptied field
                // means there is no committed query left, so the content behind the overlay must
                // fall back to the feed too, or collapsing with a blank field strands the user on
                // stale results with no way back (there is nothing left to show the clear button on).
                // A browse leaves the field empty yet is just as committed, so it shows the button too.
                if (textFieldState.text.isNotEmpty() || browsePlaceholder != null) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.clear_search_content_description)
                        )
                    }
                }
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(Res.string.search_content_description)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollapsedSearchBar(
    searchBarState: SearchBarState,
    searchBarColors: SearchBarColors,
    inputField: @Composable () -> Unit,
    onProfileClick: () -> Unit,
    backgroundColor: Color,
    showBackToDiscover: Boolean,
    onBackToDiscover: () -> Unit,
    searchBarShape: Shape,
    modifier: Modifier = Modifier,
    bottomContent: @Composable ColumnScope.() -> Unit = {}
) {
    // Mirrors the flicker guard AppBarWithSearch applies internally: hide the collapsed pill for the
    // instant currentValue has already reached Expanded but the target has moved back to Collapsed,
    // the only moment both the collapsed pill and the full-screen bar would otherwise render at once.
    val isPillVisible = searchBarState.currentValue != SearchBarValue.Expanded ||
            searchBarState.targetValue == SearchBarValue.Expanded
    val pillAlpha = if (isPillVisible) 1f else 0f

    // Mirrors [searchBarShape]'s adjoining corner (computed by the caller, alongside the input field's
    // own shape): fully rounded into a circle when the button is hidden, flat on the side facing the
    // search bar when both are visible.
    val backButtonShape = RoundedCornerShape(
        topStart = FullyRoundedCornerRadius,
        bottomStart = FullyRoundedCornerRadius,
        topEnd = AdjoiningFlatCornerRadius,
        bottomEnd = AdjoiningFlatCornerRadius
    )

    MainScreenHeader(
        onProfileClick = onProfileClick,
        modifier = modifier,
        containerColor = backgroundColor,
        leadingContent = {
            if (showBackToDiscover) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToDiscover,
                        shape = backButtonShape,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.appColors.searchBarInputFieldColor,
                            contentColor = contentColorFor(MaterialTheme.appColors.searchBarInputFieldColor)
                        ),
                        modifier = Modifier
                            .size(SearchBarDefaults.InputFieldHeight)
                            .alpha(pillAlpha)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_to_discover_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                }
            }
        },
        bottomContent = bottomContent,
        content = {
            SearchBar(
                state = searchBarState,
                inputField = inputField,
                colors = searchBarColors,
                shape = searchBarShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(pillAlpha)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpandedSearchBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    history: SearchHistoryUiModel,
    suggestions: SearchSuggestionsUiModel,
    browseGenres: List<BrowseGenreUiModel>,
    searchQuery: String,
    searchBarColors: SearchBarColors,
    onCommitSearch: (String) -> Unit,
    onBrowseGenre: (BrowseGenreUiModel) -> Unit,
    onGameClick: (Int) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    onClearRecentSearches: () -> Unit,
    onRemoveRecentSearchItem: (query: String) -> Unit
) {
    var queryToRemove by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = searchBarColors.copy(
            containerColor = MaterialTheme.appColors.expandedSearchBarColor
        )
    ) {
        // Typing owns the whole overlay: recent activity is what fills it before the first keystroke,
        // and once there is a query every row either resolves it or commits it.
        when {
            searchQuery.isNotBlank() -> SuggestionsSection(
                query = searchQuery,
                suggestions = suggestions,
                onCommitSearch = onCommitSearch,
                onGameSuggestionClick = onGameClick
            )

            browseGenres.isEmpty() && history.isEmpty -> InitialMessage()

            else -> ExpandedSearchHome(
                history = history,
                browseGenres = browseGenres,
                onBrowseGenre = onBrowseGenre,
                onCommitSearch = onCommitSearch,
                onGameClick = onGameClick,
                onRemoveRecentGame = onRemoveRecentGame,
                onClearRecentSearches = { showClearHistoryDialog = true },
                onShowRemovalDialog = { queryToRemove = it }
            )
        }

        queryToRemove?.let { query ->
            CustomAlertDialog(
                title = stringResource(Res.string.remove_history_item),
                message = annotatedStringResource(
                    Res.string.remove_history_item_message,
                    query
                ),
                confirmButtonText = stringResource(CoreUiRes.string.proceed_label),
                onConfirm = {
                    onRemoveRecentSearchItem(query)
                    queryToRemove = null
                },
                dismissButtonText = stringResource(CoreUiRes.string.cancel),
                onDismiss = { queryToRemove = null }
            )
        }

        if (showClearHistoryDialog) {
            CustomAlertDialog(
                title = stringResource(Res.string.clear_history_title),
                message = stringResource(Res.string.clear_history_message),
                confirmButtonText = stringResource(CoreUiRes.string.proceed_label),
                onConfirm = {
                    onClearRecentSearches()
                    showClearHistoryDialog = false
                },
                dismissButtonText = stringResource(CoreUiRes.string.cancel),
                onDismiss = { showClearHistoryDialog = false }
            )
        }
    }
}

@Composable
private fun SuggestionsSection(
    query: String,
    suggestions: SearchSuggestionsUiModel,
    onCommitSearch: (String) -> Unit,
    onGameSuggestionClick: (Int) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = MaterialTheme.spacing.medium)) {
        if (suggestions.historySuggestions.isNotEmpty()) {
            item(key = "header_recent") {
                SuggestionSectionHeader(
                    title = stringResource(Res.string.suggestions_section_recent)
                )
            }
            items(
                items = suggestions.historySuggestions,
                key = { "hist_$it" }
            ) { historyQuery ->
                HistorySuggestionRow(
                    query = historyQuery,
                    onClick = onCommitSearch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (suggestions.isLoadingRemote || suggestions.gameSuggestions.isNotEmpty()) {
            item(key = "header_games") {
                SuggestionSectionHeader(
                    title = stringResource(Res.string.suggestions_section_games)
                )
            }
        }

        if (suggestions.isLoadingRemote) {
            items(LOADING_SUGGESTION_COUNT, key = { "loading_$it" }) {
                LoadingSuggestionRow(modifier = Modifier.fillMaxWidth())
            }
        } else {
            items(
                items = suggestions.gameSuggestions,
                key = { "game_${it.id}" }
            ) { gameSuggestion ->
                GameSuggestionRow(
                    suggestion = gameSuggestion,
                    onClick = onGameSuggestionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "commit_search") {
            SeeAllResultsRow(
                query = query,
                onClick = onCommitSearch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val previewHistory = SearchHistoryUiModel(
    queries = listOf("The Witcher", "Cyberpunk 2077"),
    games = listOf(
        GameItemUiModel.getDummy(),
        GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
    )
)

/** The overlay at its tallest: three recent queries, the four capped games and the commit row. */
private val previewSuggestions = SearchSuggestionsUiModel(
    historySuggestions = listOf("cyberpunk", "cyberpunk mods", "cyberpunk 2078"),
    gameSuggestions = listOf(
        GameSuggestionUiModel(1, "Cyberpunk 2077", null, "CD Projekt Red · 2020"),
        GameSuggestionUiModel(2, "Cyberpunk 2077: Phantom Liberty", null, "CD Projekt Red · 2023"),
        GameSuggestionUiModel(3, "Cyberpunk 2077: Ultimate Edition", null, "CD Projekt Red · 2023"),
        GameSuggestionUiModel(4, "Cyberpunk: Edgerunners", null, "Studio Trigger")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun SearchTopBarPreview() {
    QuestLogTheme {
        SearchTopBar(
            uiState = SearchUiState(
                history = previewHistory,
                contentState = SearchContentState.Success(
                    games = listOf(GameItemUiModel.getDummy()),
                    filters = listOf(
                        GameFilterUiModel.Platform(
                            id = 0,
                            label = UiText.DynamicString("PC"),
                            selected = true
                        )
                    )
                )
            ),
            searchBarState = rememberContainedSearchBarState(),
            textFieldState = rememberTextFieldState("The Witcher"),
            scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
            onSearch = {},
            onGameClick = {},
            onBrowseGenre = {},
            onEvent = {},
            onProfileClick = {},
            backgroundColor = Color.Transparent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun SearchInputFieldPreview() {
    QuestLogTheme {
        SearchInputField(
            textFieldState = rememberTextFieldState("The Witcher"),
            searchBarState = rememberContainedSearchBarState(),
            onSearch = {},
            onClearSearch = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun SearchInputFieldGenreBrowsePreview() {
    QuestLogTheme {
        SearchInputField(
            textFieldState = rememberTextFieldState(),
            searchBarState = rememberContainedSearchBarState(),
            onSearch = {},
            onClearSearch = {},
            browsePlaceholder = UiText.DynamicString("genre: RPG")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun CollapsedSearchBarPreview() {
    QuestLogTheme {
        val searchBarState = rememberContainedSearchBarState()
        val textFieldState = rememberTextFieldState("The Witcher")

        CollapsedSearchBar(
            searchBarState = searchBarState,
            searchBarColors = SearchBarDefaults.colors(),
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            onProfileClick = {},
            backgroundColor = Color.Transparent,
            showBackToDiscover = false,
            onBackToDiscover = {},
            searchBarShape = RoundedCornerShape(FullyRoundedCornerRadius)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun CollapsedSearchBarWithBackButtonPreview() {
    QuestLogTheme {
        val searchBarState = rememberContainedSearchBarState()
        val textFieldState = rememberTextFieldState("The Witcher")

        CollapsedSearchBar(
            searchBarState = searchBarState,
            searchBarColors = SearchBarDefaults.colors(),
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            onProfileClick = {},
            backgroundColor = Color.Transparent,
            showBackToDiscover = true,
            onBackToDiscover = {},
            searchBarShape = RoundedCornerShape(
                topStart = AdjoiningFlatCornerRadius,
                bottomStart = AdjoiningFlatCornerRadius,
                topEnd = FullyRoundedCornerRadius,
                bottomEnd = FullyRoundedCornerRadius
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun ExpandedSearchBarPreview() {
    QuestLogTheme {
        val searchBarState = rememberContainedSearchBarState(SearchBarValue.Expanded)
        val textFieldState = rememberTextFieldState()

        ExpandedSearchBar(
            searchBarState = searchBarState,
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            history = previewHistory,
            suggestions = SearchSuggestionsUiModel(),
            browseGenres = defaultBrowseGenres(),
            searchQuery = "",
            searchBarColors = SearchBarDefaults.colors(),
            onCommitSearch = {},
            onBrowseGenre = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onRemoveRecentSearchItem = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun ExpandedSearchBarFirstTimePreview() {
    QuestLogTheme {
        val searchBarState = rememberContainedSearchBarState(SearchBarValue.Expanded)
        val textFieldState = rememberTextFieldState()

        ExpandedSearchBar(
            searchBarState = searchBarState,
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            history = SearchHistoryUiModel(),
            suggestions = SearchSuggestionsUiModel(),
            browseGenres = defaultBrowseGenres(),
            searchQuery = "",
            searchBarColors = SearchBarDefaults.colors(),
            onCommitSearch = {},
            onBrowseGenre = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onRemoveRecentSearchItem = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun ExpandedSearchBarTypingPreview() {
    QuestLogTheme {
        val searchBarState = rememberContainedSearchBarState(SearchBarValue.Expanded)
        val textFieldState = rememberTextFieldState("cyberpunk")

        ExpandedSearchBar(
            searchBarState = searchBarState,
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            history = previewHistory,
            suggestions = previewSuggestions,
            browseGenres = defaultBrowseGenres(),
            searchQuery = "cyberpunk",
            searchBarColors = SearchBarDefaults.colors(),
            onCommitSearch = {},
            onBrowseGenre = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onRemoveRecentSearchItem = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@QuestLogPreviews
@Composable
private fun ExpandedSearchBarLoadingPreview() {
    QuestLogTheme {
        val searchBarState = rememberContainedSearchBarState(SearchBarValue.Expanded)
        val textFieldState = rememberTextFieldState("cyberpunk")

        ExpandedSearchBar(
            searchBarState = searchBarState,
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            history = previewHistory,
            suggestions = previewSuggestions.copy(
                gameSuggestions = emptyList(),
                isLoadingRemote = true
            ),
            browseGenres = defaultBrowseGenres(),
            searchQuery = "cyberpunk",
            searchBarColors = SearchBarDefaults.colors(),
            onCommitSearch = {},
            onBrowseGenre = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onRemoveRecentSearchItem = {}
        )
    }
}

package com.example.gameswishlist.feature.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import com.example.gameswishlist.feature.search.R as SearchR

import com.example.gameswishlist.core.ui.util.annotatedStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTopBar(
    uiState: SearchUiState,
    searchBarState: SearchBarState,
    scrollBehavior: SearchBarScrollBehavior,
    onSearch: (String) -> Unit,
    onEvent: (SearchUiEvent) -> Unit
) {
    val isScrolled by remember(scrollBehavior) {
        derivedStateOf {
            if (scrollBehavior.scrollOffsetLimit != 0f) {
                val fraction = 1 - ((scrollBehavior.scrollOffsetLimit - scrollBehavior.contentOffset).coerceIn(
                    scrollBehavior.scrollOffsetLimit,
                    0f
                ) / scrollBehavior.scrollOffsetLimit)
                fraction > 0.01f
            } else false
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.appColors.searchBarScrolledContainerColor
        else MaterialTheme.appColors.appBackground,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "topBarBackground"
    )

    val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
        searchBarColors = SearchBarDefaults.containedColors(state = searchBarState).copy(
            dividerColor = SearchBarDefaults.colors().dividerColor.copy(alpha = 0.5f)
        ),
        appBarContainerColor = Color.Transparent,
        scrolledAppBarContainerColor = Color.Transparent,
    )

    // Creiamo un proxy per far sì che la SearchBar interna veda lo stato dello scroll (per i colori)
    // ma non applichi il modificatore di scroll, poiché lo applichiamo noi alla Surface esterna.
    val proxyScrollBehavior = remember(scrollBehavior) {
        object : SearchBarScrollBehavior by scrollBehavior {
            override fun Modifier.searchBarScrollBehavior(): Modifier = this
        }
    }

    val textFieldState = androidx.compose.foundation.text.input.rememberTextFieldState()

    val inputField = @Composable {
        SearchInputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { onSearch(textFieldState.text.toString()) },
            showFilterIcon = uiState.contentState is SearchContentState.Success,
            onFilterClick = { onEvent(SearchUiEvent.OnOpenFilters) }
        )
    }

    Surface(
        color = backgroundColor,
        modifier = with(scrollBehavior) { Modifier.searchBarScrollBehavior() }
    ) {
        Column {
            CollapsedSearchBar(
                searchBarState = searchBarState,
                scrollBehavior = proxyScrollBehavior,
                appBarWithSearchColors = appBarWithSearchColors,
                inputField = inputField
            )

            val state = uiState.contentState
            if (state is SearchContentState.Success || state is SearchContentState.Loading) {
                if (state is SearchContentState.Success) {
                    FilterChipsRow(
                        filters = state.filters,
                        onFilterClick = { onEvent(SearchUiEvent.OnFilterClick(it)) }
                    )
                } else {
                    FilterChipsSkeletonRow()
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            }
        }
    }

    ExpandedSearchBar(
        searchBarState = searchBarState,
        inputField = inputField,
        recentSearches = uiState.recentSearches,
        appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
            searchBarColors = appBarWithSearchColors.searchBarColors,
            appBarContainerColor = MaterialTheme.appColors.appBackground,
            scrolledAppBarContainerColor = MaterialTheme.appColors.searchBarScrolledContainerColor
        ),
        onHistoryItemClicked = { onSearch(it) },
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
    showFilterIcon: Boolean = false,
    onFilterClick: () -> Unit = {}
) {
    val searchInputFieldColor = if (searchBarState.currentValue == SearchBarValue.Collapsed) {
        MaterialTheme.appColors.searchBarInputFieldColor
    } else {
        MaterialTheme.appColors.expandedSearchBarColor
    }

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = searchInputFieldColor,
            unfocusedContainerColor = searchInputFieldColor
        ),
        onSearch = { onSearch() },
        readOnly = searchBarState.currentValue == SearchBarValue.Collapsed,
        placeholder = {
            Text(
                modifier = Modifier.clearAndSetSemantics {},
                text = stringResource(SearchR.string.search_placeholder)
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (searchBarState.currentValue == SearchBarValue.Collapsed && showFilterIcon) {
                    IconButton(onClick = onFilterClick) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Open filters")
                    }
                }
                IconButton(onClick = onSearch) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsedSearchBar(
    searchBarState: SearchBarState,
    scrollBehavior: SearchBarScrollBehavior,
    appBarWithSearchColors: AppBarWithSearchColors,
    inputField: @Composable () -> Unit
) {
    AppBarWithSearch(
        scrollBehavior = scrollBehavior,
        state = searchBarState,
        colors = appBarWithSearchColors,
        inputField = inputField,
        contentPadding = PaddingValues(
            bottom = MaterialTheme.spacing.large,
            top = WindowInsets.statusBars.asPaddingValues()
                .calculateTopPadding() + MaterialTheme.spacing.large
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedSearchBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    recentSearches: List<String>,
    appBarWithSearchColors: AppBarWithSearchColors,
    onHistoryItemClicked: (query: String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onRemoveRecentSearchItem: (query: String) -> Unit
) {
    var showHistoryItemRemovalDialog by remember { mutableStateOf(false) }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = appBarWithSearchColors.searchBarColors.copy(
            containerColor = MaterialTheme.appColors.expandedSearchBarColor
        )
    ) {
        if (recentSearches.isEmpty()) return@ExpandedFullScreenSearchBar
        var recentSearchToBeRemoved by remember { mutableStateOf("") }

        fun showRecentSearchRemovalDialog(itemToRemove: String) {
            recentSearchToBeRemoved = itemToRemove
            showHistoryItemRemovalDialog = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = MaterialTheme.spacing.medium
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
            ) {
                Text(
                    text = stringResource(SearchR.string.recent_searches),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(
                    onClick = onClearRecentSearches
                ) {
                    Text(
                        text = stringResource(SearchR.string.clear_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.appColors.onAppBackground
                    )
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
            ) {
                items(items = recentSearches, key = { it }) { recentSearch ->
                    val inputChipInteractionSource = remember { MutableInteractionSource() }
                    Box {
                        SuggestionChip(
                            onClick = { onHistoryItemClicked(recentSearch) },
                            label = {
                                Text(
                                    text = recentSearch,
                                    maxLines = 1
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null
                                )
                            },
                            contentPadding = PaddingValues(all = MaterialTheme.spacing.small),
                            interactionSource = inputChipInteractionSource
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .combinedClickable(
                                    onLongClick = { showRecentSearchRemovalDialog(recentSearch) },
                                    onClick = { onHistoryItemClicked(recentSearch) },
                                    interactionSource = inputChipInteractionSource,
                                    indication = null,
                                )
                        )
                    }
                }
            }
        }

        if (showHistoryItemRemovalDialog) {
            CustomAlertDialog(
                title = stringResource(SearchR.string.remove_history_item),
                message = annotatedStringResource(
                    SearchR.string.remove_history_item_message,
                    recentSearchToBeRemoved
                ),
                confirmButtonText = stringResource(SearchR.string.proceed_label),
                onConfirm = { onRemoveRecentSearchItem(recentSearchToBeRemoved) },
                dismissButtonText = stringResource(SearchR.string.cancel),
                onDismiss = { showHistoryItemRemovalDialog = false }
            )
        }
    }
}

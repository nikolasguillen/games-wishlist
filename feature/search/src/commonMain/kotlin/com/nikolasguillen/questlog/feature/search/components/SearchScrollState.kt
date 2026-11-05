package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.runtime.Stable

/**
 * What the Search screen scrolls and how: the two lists it can show (the results grid and the Discover feed), the
 * search bar's scroll behavior, and the scroll-to-top button that follows whichever list is on screen.
 *
 * The screen owns these because the layouts only arrange them, and because the button, the bar and the lists have
 * to agree on which list is current.
 *
 * @property showScrollToTop Whether the scroll-to-top button is visible.
 * @property onScrollToTop Scrolls the list on screen back to its top and brings the search bar back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
internal class SearchScrollState(
    val scrollBehavior: SearchBarScrollBehavior,
    val gridState: LazyGridState,
    val discoverListState: LazyListState,
    val showScrollToTop: Boolean,
    val onScrollToTop: () -> Unit
)

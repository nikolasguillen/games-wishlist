package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.ui.Modifier

// iOS builds against the Material 3 alpha, where the scroll offsets are properties of the behavior itself and the
// modifier is a function; Android's newer release moved them behind `scrollState` and a property.

@OptIn(ExperimentalMaterial3Api::class)
internal actual fun SearchBarScrollBehavior.resetToTop() {
    contentOffset = 0f
    scrollOffset = 0f
}

@OptIn(ExperimentalMaterial3Api::class)
internal actual val SearchBarScrollBehavior.barModifier: Modifier
    get() = with(this) { Modifier.searchBarScrollBehavior() }

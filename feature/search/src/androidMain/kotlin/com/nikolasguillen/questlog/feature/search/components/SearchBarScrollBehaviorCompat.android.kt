package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
internal actual fun SearchBarScrollBehavior.resetToTop() {
    scrollState.contentOffset = 0f
    scrollState.scrollOffset = 0f
}

@OptIn(ExperimentalMaterial3Api::class)
internal actual val SearchBarScrollBehavior.barModifier: Modifier
    get() = searchBarScrollBehaviorModifier

package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.ui.Modifier

/**
 * The two `SearchBarScrollBehavior` members this screen uses that exist in Material 3 1.5.0-beta01 (what Android
 * runs) but not in the alpha the multiplatform artifact is built from, so common code cannot call them. Each
 * platform reaches its own version of Material 3.
 */

/** Scrolls the bar back to fully visible, as if the list were at its top. */
@OptIn(ExperimentalMaterial3Api::class)
internal expect fun SearchBarScrollBehavior.resetToTop()

/** The modifier that lets the search bar follow the scroll behavior. */
@OptIn(ExperimentalMaterial3Api::class)
internal expect val SearchBarScrollBehavior.barModifier: Modifier

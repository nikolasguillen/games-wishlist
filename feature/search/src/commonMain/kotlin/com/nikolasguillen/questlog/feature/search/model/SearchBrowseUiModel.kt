package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * What the committed results grid is showing when it is not the result of a typed query.
 *
 * Both cases render through [SearchContentState] like a text search and leave the text field empty, so the
 * search bar's placeholder is what says what is on screen. They differ in where the games come from: a
 * [Genre] browse is fetched, a [Shelf] grid is a snapshot of games the Discover feed already holds.
 */
@Immutable
internal sealed interface SearchBrowseUiModel {
    /** The popular, recent, released games of one genre (spec 014). */
    data class Genre(val genre: BrowseGenreUiModel) : SearchBrowseUiModel

    /**
     * Every game of one Discover shelf. [title] is the shelf's own heading, so the search bar names exactly
     * what the user tapped.
     */
    data class Shelf(val key: DiscoverShelfKey, val title: UiText) : SearchBrowseUiModel
}

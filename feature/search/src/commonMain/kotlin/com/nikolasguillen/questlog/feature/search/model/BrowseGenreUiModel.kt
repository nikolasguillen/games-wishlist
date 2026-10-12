package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * One genre chip in the "Browse by genre" row, and the genre a browse is showing.
 *
 * @property id The IGDB genre id. This is what code compares and what the browse is queried by.
 * @property label The short name for display only.
 */
@Immutable
internal data class BrowseGenreUiModel(
    val id: Int,
    val label: UiText
)

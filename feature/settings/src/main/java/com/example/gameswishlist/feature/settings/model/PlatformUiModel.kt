package com.example.gameswishlist.feature.settings.model

import androidx.compose.runtime.Immutable

/**
 * A platform as the picker's list shows it.
 *
 * @property id IGDB's identifier — the toggle and the stored selection travel on this, never on the
 * name. The search box matches on text, but nothing downstream does.
 * @property name The platform's full name, which is what reads well down a column.
 * @property abbreviation Shown as a secondary line when IGDB has one, so "PS5" stays findable next to
 * "PlayStation 5". Data-derived, hence a plain `String`.
 */
@Immutable
internal data class PlatformUiModel(
    val id: Int,
    val name: String,
    val abbreviation: String?,
    val isSelected: Boolean
)

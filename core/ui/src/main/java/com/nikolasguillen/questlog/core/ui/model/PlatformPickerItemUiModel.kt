package com.nikolasguillen.questlog.core.ui.model

import androidx.compose.runtime.Immutable

/**
 * A platform as the owned-platforms picker's list shows it, in Settings and in the welcome flow.
 *
 * @property id IGDB's identifier — the toggle and the stored selection travel on this, never on the
 * name. The search box matches on text, but nothing downstream does.
 * @property name The platform's full name, which is what reads well down a column.
 * @property abbreviation Shown as a secondary line when IGDB has one, so "PS5" stays findable next to
 * "PlayStation 5". Data-derived, hence a plain `String`.
 */
@Immutable
data class PlatformPickerItemUiModel(
    val id: Int,
    val name: String,
    val abbreviation: String?,
    val isSelected: Boolean
)

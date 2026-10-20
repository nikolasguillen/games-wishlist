package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * What the header of a character or developer page shows besides the game count, which arrives with the games.
 *
 * @property name The character's or developer's name.
 * @property imageUrl The portrait or logo, or `null` for the placeholder.
 * @property foundedYearText The "Founded <year>" chip. Always `null` for a character and for a developer whose
 * founding date the catalogue lacks.
 */
@Immutable
internal data class EntityHeaderUiModel(
    val name: String,
    val imageUrl: String?,
    val foundedYearText: UiText?
)

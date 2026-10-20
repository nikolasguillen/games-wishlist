package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * One card of a Characters or Developers results grid. Whether it is drawn as a portrait or as a logo comes from
 * [SearchUiState.resultsType], not from the model.
 *
 * @property id The character's or company's catalogue id; what a tap navigates with.
 * @property name The name under the card.
 * @property imageUrl The portrait or logo, or `null` for the placeholder.
 */
@Immutable
internal data class EntityResultUiModel(
    val id: Int,
    val name: String,
    val imageUrl: String?
)

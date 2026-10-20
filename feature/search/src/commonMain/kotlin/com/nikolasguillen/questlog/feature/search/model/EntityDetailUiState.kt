package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.DescriptionTranslationState
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * State of a character or developer page.
 *
 * The page has two content areas that load independently, so it carries two content states: [profile] (the header
 * and the description) and [games].
 *
 * @property subject Whose page this is.
 * @property gameCountText The "N games" chip, set once the games are known (and so absent while they load).
 * @property descriptionTranslation The on-device translation lifecycle of the description.
 * @property listSelectorState State of the list-selector bottom sheet; `null` means it is closed.
 */
@Immutable
internal data class EntityDetailUiState(
    val subject: EntityDetailSubject,
    val profile: EntityProfileContentState = EntityProfileContentState.Loading,
    val games: EntityGamesContentState = EntityGamesContentState.Loading,
    val gameCountText: UiText? = null,
    val descriptionTranslation: DescriptionTranslationState = DescriptionTranslationState.Unavailable,
    val listSelectorState: ListSelectorState? = null
)

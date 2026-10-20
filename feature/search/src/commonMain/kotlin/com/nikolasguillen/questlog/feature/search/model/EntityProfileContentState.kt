package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/** Lifecycle of the header and description of a character or developer page. */
@Immutable
internal sealed interface EntityProfileContentState {
    data object Loading : EntityProfileContentState

    data class Error(val message: UiText) : EntityProfileContentState

    /** [description] is `null` when the catalogue has none, and the page then draws no description card. */
    data class Success(
        val header: EntityHeaderUiModel,
        val description: String?
    ) : EntityProfileContentState
}

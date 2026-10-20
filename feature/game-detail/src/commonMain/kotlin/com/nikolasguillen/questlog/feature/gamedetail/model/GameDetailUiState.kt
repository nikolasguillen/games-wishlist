package com.nikolasguillen.questlog.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.DescriptionTranslationState

@Immutable
internal data class GameDetailUiState(
    val contentState: GameDetailContentState = GameDetailContentState.Loading,
    val wishlistSelectorState: WishlistSelectorState? = null,
    val descriptionTranslation: DescriptionTranslationState = DescriptionTranslationState.Unavailable
)

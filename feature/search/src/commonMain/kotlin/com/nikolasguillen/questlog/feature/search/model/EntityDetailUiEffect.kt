package com.nikolasguillen.questlog.feature.search.model

import com.nikolasguillen.questlog.core.ui.model.UiText

internal sealed interface EntityDetailUiEffect {
    data class ShowSnackbar(val message: UiText) : EntityDetailUiEffect
}

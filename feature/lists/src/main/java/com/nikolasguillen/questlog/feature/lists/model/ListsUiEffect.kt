package com.nikolasguillen.questlog.feature.lists.model

import com.nikolasguillen.questlog.core.ui.model.UiText

internal sealed interface ListsUiEffect {
    data class ShowSnackbar(val message: UiText) : ListsUiEffect
}

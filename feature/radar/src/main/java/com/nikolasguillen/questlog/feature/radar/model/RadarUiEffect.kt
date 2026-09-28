package com.nikolasguillen.questlog.feature.radar.model

import com.nikolasguillen.questlog.core.ui.model.UiText

internal sealed interface RadarUiEffect {
    /** An opt-in was just turned on; ask for permission if it is not already granted. */
    data object RequestNotificationPermission : RadarUiEffect

    data class ShowSnackbar(val message: UiText) : RadarUiEffect
}

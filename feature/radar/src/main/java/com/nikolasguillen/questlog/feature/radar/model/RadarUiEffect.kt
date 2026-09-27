package com.nikolasguillen.questlog.feature.radar.model

internal sealed interface RadarUiEffect {
    /** An opt-in was just turned on; ask for permission if it is not already granted. */
    data object RequestNotificationPermission : RadarUiEffect
}

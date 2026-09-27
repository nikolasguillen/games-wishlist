package com.nikolasguillen.questlog.feature.radar.model

internal sealed interface RadarUiEvent {
    /** The bell on a row was tapped. [gameId], not the row: a multi-platform game's rows share one opt-in. */
    data class ToggleReleaseNotification(val gameId: Int) : RadarUiEvent
}

package com.example.gameswishlist.feature.radar.model

import androidx.compose.runtime.Immutable

/**
 * Content lifecycle of the Radar timeline.
 *
 * There is no `Error` case: the timeline is built off `GetRadarTimelineUseCase`, which observes local
 * storage and has no failure channel — a failed background refresh just leaves the dates as they were.
 */
@Immutable
internal sealed interface RadarContentState {
    data object Loading : RadarContentState
    data object Empty : RadarContentState
    data class Success(val sections: List<RadarSectionUiModel>) : RadarContentState
}

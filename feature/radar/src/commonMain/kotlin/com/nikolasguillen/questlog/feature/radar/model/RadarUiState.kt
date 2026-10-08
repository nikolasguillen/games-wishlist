package com.nikolasguillen.questlog.feature.radar.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class RadarUiState(
    val contentState: RadarContentState = RadarContentState.Loading,
    /** Whether the platform offers release reminders; when `false` no row shows its bell toggle. */
    val releaseRemindersAvailable: Boolean = true
)
